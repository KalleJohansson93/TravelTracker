// Uppdaterade imports för Functions V2
import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
// Importera typer för V2 Firestore triggers
import {Change, DocumentSnapshot, FirestoreEvent, DocumentOptions} from "firebase-functions/v2/firestore";

// *** KORRIGERA IMPORTEN FÖR getFirestore ***
// Importera getFirestore från firebase-admin/firestore
import {getFirestore} from "firebase-admin/firestore";


// Initiera Firebase Admin SDK.
admin.initializeApp();

// *** DETTA ÄR KODEN SOM PEKAR PÅ "traveltracker" ***
// Använd getFirestore() för att få Admin SDK-instansen för den specifika databasen.
// Den behöver en Firebase App-instans (använd standard-appen) och databaseId.
const firestoreDB = getFirestore(admin.app(), "traveltracker");


// Data interface som matchar din UserCountryData struktur i Firestore
interface UserCountryData {
    status?: string; // "VISITED", "WANT_TO_VISIT", "NOT_VISITED", eller null
    rating?: number | null; // 1-10, null, eller undefined
    // Lägg till andra fält om du har dem, t.ex. lastUpdated
}

// Data interface för aggregeringar per land
interface CountryAggregate {
    visitedCount: number;
    ratingSum: number;
    ratingCount: number;
}

// Data interface för global statistik (topplistor) som appen läser
interface GlobalStats {
    mostVisited: { countryCode: string; count: number; }[];
    highestRated: { countryCode: string; averageRating: number; }[];
    lastCalculated: admin.firestore.Timestamp;
}

/**
 * Triggas när ett dokument i /users/{userId}/userCountries/{countryCode} ändras (skapas, uppdateras, raderas).
 * Uppdaterar aggregerade räknare/summor för landet och beräknar om global statistik.
 */
export const aggregateUserCountryWrite = functions.firestore
  .onDocumentWritten(
        {
          document: "/users/{userId}/userCountries/{countryCode}",
          region: "europe-west3", // Behåll din region
          database: "traveltracker", // Specificerar databas-ID FÖR TRIGGERN
        } as DocumentOptions<string>,

        async (event: FirestoreEvent<Change<DocumentSnapshot> | undefined>) => {
          const change = event.data;
          if (!change) {
            functions.logger.error("No change data in event.");
            return null;
          }
          const countryCode = event.params.countryCode as string;

          const beforeSnapshot = change.before;
          const afterSnapshot = change.after;
          const beforeData = beforeSnapshot.exists ? (beforeSnapshot.data() as UserCountryData) : null;
          const afterData = afterSnapshot.exists ? (afterSnapshot.data() as UserCountryData) : null;
          const isDeleted = !afterSnapshot.exists;

          const oldStatus = beforeData?.status;
          const newStatus = afterData?.status;
          const oldRating = beforeData?.rating ?? null;
          const newRating = afterData?.rating ?? null;

          let visitedChange = 0;
          if (oldStatus !== "VISITED" && newStatus === "VISITED") visitedChange = 1;
          else if (oldStatus === "VISITED" && newStatus !== "VISITED" && newStatus !== undefined) visitedChange = -1;
          else if (oldStatus === "VISITED" && isDeleted) visitedChange = -1;

          let ratingSumChange = 0;
          let ratingCountChange = 0;
          if (oldRating == null && newRating != null) {
            ratingSumChange = newRating; ratingCountChange = 1;
          } else if (oldRating != null && newRating == null) {
            ratingSumChange = -oldRating; ratingCountChange = -1;
          } else if (oldRating != null && newRating != null && oldRating !== newRating) {
            ratingSumChange = newRating - oldRating; ratingCountChange = 0;
          } else if (oldRating != null && isDeleted) {
            ratingSumChange = -oldRating; ratingCountChange = -1;
          }


          if (visitedChange === 0 && ratingSumChange === 0 && ratingCountChange === 0) {
            functions.logger.log(`No relevant change for ${countryCode}. Skipping aggregation.`);
            return null;
          }

          // --- KÖR TRANSAKTION ---
          const aggregateDocRef = firestoreDB.collection("countryAggregates").doc(countryCode);
          const globalStatsDocRef = firestoreDB.collection("statistics").doc("globalStats");

          try {
            await firestoreDB.runTransaction(async (transaction: admin.firestore.Transaction) => {
              // *** STEG 1: ALLA LÄSÅTGÄRDER FÖRST ***
              const aggregateDoc = await transaction.get(aggregateDocRef) as admin.firestore.DocumentSnapshot;
              const allAggregatesSnapshot = await transaction.get(firestoreDB.collection("countryAggregates"));
              const globalStatsDoc = await transaction.get(globalStatsDocRef) as admin.firestore.DocumentSnapshot;


              // *** STEG 2: BEARBETNING AV LÄST DATA ***
              const currentAggregate = (aggregateDoc.data() as CountryAggregate | undefined) || {visitedCount: 0, ratingSum: 0, ratingCount: 0};
              const allAggregates = allAggregatesSnapshot.docs.map((doc: admin.firestore.QueryDocumentSnapshot) => ({countryCode: doc.id, ...doc.data() as CountryAggregate}));
              const currentGlobalStats = (globalStatsDoc.data() as GlobalStats | undefined) || {mostVisited: [], highestRated: [], lastCalculated: admin.firestore.Timestamp.now()};

              // *** STEG 3: BERÄKNA NYA VÄRDEN ***
              // Uppdatera aggregeringen för det specifika landet
              currentAggregate.visitedCount += visitedChange;
              currentAggregate.ratingSum += ratingSumChange;
              currentAggregate.ratingCount += ratingCountChange;
              currentAggregate.visitedCount = Math.max(0, currentAggregate.visitedCount);
              currentAggregate.ratingCount = Math.max(0, currentAggregate.ratingCount);
              if (currentAggregate.ratingCount === 0) {
                currentAggregate.ratingSum = 0;
              }

              // Beräkna topp 5 mest besökta från ALLA aggregeringar
              const mostVisited = allAggregates
                .filter((agg: CountryAggregate) => agg.visitedCount > 0)
                .sort((a: { visitedCount: number }, b: { visitedCount: number }) => b.visitedCount - a.visitedCount) // Sort här
                .slice(0, 5)
                .map((agg) => ({countryCode: agg.countryCode, count: agg.visitedCount}));

              // Beräkna topp 5 högst betygsatta från ALLA aggregeringar
              const highestRated = allAggregates
                .filter((agg: { ratingCount: number }) => agg.ratingCount > 0) // Filter här
                .map((agg) => ({ // Map här (beräknar medelvärde)
                  countryCode: agg.countryCode, // Explicit inkludera countryCode
                  averageRating: agg.ratingSum / agg.ratingCount,
                }))
                .sort((a: { averageRating: number }, b: { averageRating: number }) => b.averageRating - a.averageRating) // Sort här
                .slice(0, 5)
                .map((agg) => ({countryCode: agg.countryCode, averageRating: agg.averageRating})); // Final map för output


              // *** STEG 4: ALLA SKRIVÅTGÄRDER SIST ***
              // Uppdatera globalStats med de nya topplistorna
              currentGlobalStats.mostVisited = mostVisited;
              currentGlobalStats.highestRated = highestRated;
              currentGlobalStats.lastCalculated = admin.firestore.Timestamp.now();

              // Skriv tillbaka det uppdaterade aggregeringsdokumentet för landet
              transaction.set(aggregateDocRef, currentAggregate);

              // Skriv tillbaka det uppdaterade globalStats dokumentet
              transaction.set(globalStatsDocRef, currentGlobalStats);


              functions.logger.log(`Successfully updated aggregates for ${countryCode} and recalculated global stats.`);
            }); // Slut på transaktionen

            return null;
          } catch (error) {
            functions.logger.error(`Transaction failed for ${countryCode}:`, error);
            // Logga mer detaljer om felet om möjligt
            if (error instanceof Error) {
              functions.logger.error(`Error details: ${error.name} - ${error.message}`, error.stack);
              // För Firestore-specifika fel, kan du ibland komma åt mer info
              // if ('code' in error) functions.logger.error(`Firestore Error Code: ${error.code}`);
            }
            return null;
          }
        }
  );
