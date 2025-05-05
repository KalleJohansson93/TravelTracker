// Uppdaterade imports för Functions V2
import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
// Importera typer för V2 Firestore triggers
import {Change, DocumentSnapshot, FirestoreEvent, DocumentOptions} from "firebase-functions/v2/firestore";

import {getFirestore} from "firebase-admin/firestore";
import {onSchedule} from "firebase-functions/v2/scheduler";

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
    wantedCount: number;
}

// Data interface för global statistik (topplistor) som appen läser
interface GlobalStats {
    mostVisited: { countryCode: string; count: number; }[];
    highestRated: { countryCode: string; averageRating: number; }[];
    mostWanted: { countryCode: string; count: number; }[];
    topUsersVisited: { userId: string; username: string; count: number; }[];
    lastCalculated: admin.firestore.Timestamp;
}

interface UserProfile {
    username: string;
    visitedCountriesCount?: number;
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
          const userId = event.params.userId as string;

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

          let wantedChange = 0;
          if (oldStatus !== "WANT_TO_VISIT" && newStatus === "WANT_TO_VISIT") wantedChange = 1;
          else if (oldStatus === "WANT_TO_VISIT" && newStatus !== "WANT_TO_VISIT" && newStatus !== undefined) wantedChange = -1;
          else if (oldStatus === "WANT_TO_VISIT" && isDeleted) wantedChange = -1;

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

          if (visitedChange === 0 && wantedChange === 0 && ratingSumChange === 0 && ratingCountChange === 0) {
            functions.logger.log(`No relevant change for ${countryCode} affecting country aggregates. Skipping aggregation.`);
            // *** VIKTIGT: Fortsätt KANSKE om visitedChange är > 0 eller < 0 men wanted/rating är 0 ***
            // Detta är för att uppdatera användarens räknare även om landets aggregering inte ändras
            if (visitedChange === 0) {
              functions.logger.log(`No change in visited status for ${countryCode} for user ${userId}. Skipping user count update.`);
              return null; // Skippa HELA funktionen om inget relevant ändras
            }
            functions.logger.log(`Visited status changed for ${countryCode} for user ${userId}. Proceeding to update user count.`);
          } else {
            functions.logger.log(`Relevant change for ${countryCode} affecting country aggregates. Proceeding with aggregation.`);
          }

          // --- KÖR TRANSAKTION ---
          const aggregateDocRef = firestoreDB.collection("countryAggregates").doc(countryCode);
          const globalStatsDocRef = firestoreDB.collection("statistics").doc("globalStats");
          const userDocRef = firestoreDB.collection("users").doc(userId); // *** NY REFERENS TILL ANVÄNDARENS DOKUMENT ***


          try {
            await firestoreDB.runTransaction(async (transaction: admin.firestore.Transaction) => {
              // *** STEG 1: ALLA LÄSÅTGÄRDER FÖRST ***
              const aggregateDoc = await transaction.get(aggregateDocRef) as admin.firestore.DocumentSnapshot;
              const allAggregatesSnapshot = await transaction.get(firestoreDB.collection("countryAggregates"));
              const globalStatsDoc = await transaction.get(globalStatsDocRef) as admin.firestore.DocumentSnapshot;
              const userDoc = await transaction.get(userDocRef) as admin.firestore.DocumentSnapshot; // *** NY LÄSNING: ANVÄNDARENS DOKUMENT ***


              // *** STEG 2: BEARBETNING AV LÄST DATA ***
              const currentAggregateData = aggregateDoc.data();
              const currentAggregate: CountryAggregate = {
                visitedCount: currentAggregateData?.visitedCount ?? 0,
                ratingSum: currentAggregateData?.ratingSum ?? 0,
                ratingCount: currentAggregateData?.ratingCount ?? 0,
                wantedCount: currentAggregateData?.wantedCount ?? 0,
              };
              const allAggregates = allAggregatesSnapshot.docs.map((doc: admin.firestore.QueryDocumentSnapshot) => ({countryCode: doc.id, ...doc.data() as CountryAggregate}));
              const currentGlobalStats = (globalStatsDoc.data() as GlobalStats | undefined) || {mostVisited: [], highestRated: [], mostWanted: [], lastCalculated: admin.firestore.Timestamp.now()};

              const currentUserData = userDoc.data() as UserProfile | undefined;
              const currentUserVisitedCount = currentUserData?.visitedCountriesCount ?? 0;

              // *** STEG 3: BERÄKNA NYA VÄRDEN ***
              // Uppdatera aggregeringen för det specifika landet
              currentAggregate.visitedCount += visitedChange;
              currentAggregate.wantedCount += wantedChange;
              currentAggregate.ratingSum += ratingSumChange;
              currentAggregate.ratingCount += ratingCountChange;
              currentAggregate.visitedCount = Math.max(0, currentAggregate.visitedCount);
              currentAggregate.wantedCount = Math.max(0, currentAggregate.wantedCount);
              currentAggregate.ratingCount = Math.max(0, currentAggregate.ratingCount);
              if (currentAggregate.ratingCount === 0) {
                currentAggregate.ratingSum = 0;
              }

              const newUserVisitedCount = currentUserVisitedCount + visitedChange;
              const finalUserVisitedCount = Math.max(0, newUserVisitedCount);

              // Beräkna topp 5 mest besökta från ALLA aggregeringar
              const mostVisited = allAggregates
                .filter((agg: CountryAggregate) => agg.visitedCount > 0)
                .sort((a: { visitedCount: number }, b: { visitedCount: number }) => b.visitedCount - a.visitedCount) // Sort här
                .slice(0, 5)
                .map((agg) => ({countryCode: agg.countryCode, count: agg.visitedCount}));

              const mostWanted = allAggregates
                .filter((agg: CountryAggregate) => agg.wantedCount > 0) // Filtrera på wantedCount
                .sort((a: { wantedCount: number }, b: { wantedCount: number }) => b.wantedCount - a.wantedCount) // Sortera på wantedCount
                .slice(0, 5)
                .map((agg) => ({countryCode: agg.countryCode, count: agg.wantedCount})); // Returnera count för wanted

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

              if (visitedChange !== 0 || wantedChange !== 0 || ratingSumChange !== 0 || ratingCountChange !== 0) {
                transaction.set(aggregateDocRef, currentAggregate);
              } else {
                // Om ingen ändring påverkar landets aggregering, skriv inte om dokumentet
                // Detta sparar en skrivning om t.ex. bara användarens räknare behöver uppdateras (om vi inte skippade funktionen helt)
              }

              // Uppdatera globalStats med de nya topplistorna
              currentGlobalStats.mostVisited = mostVisited;
              currentGlobalStats.mostWanted = mostWanted;
              currentGlobalStats.highestRated = highestRated;
              currentGlobalStats.lastCalculated = admin.firestore.Timestamp.now();

              // Skriv tillbaka det uppdaterade globalStats dokumentet
              // Vi använder { merge: true } för att inte skriva över fält som t.ex. topUsersVisited
              transaction.set(globalStatsDocRef, currentGlobalStats, {merge: true});


              // *** NY SKRIVNING: Uppdatera användarens dokument med nya räknaren ***
              // Använd update istället för set för att bara ändra fältet visitedCountriesCount
              transaction.update(userDocRef, {visitedCountriesCount: finalUserVisitedCount});


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

// *** STEG 2: SKAPA EN NY SCHEMALAGD FUNKTION FÖR TOPP 5 ANVÄNDARE ***

/**
 * Schemalagd funktion som beräknar de topp 5 användarna baserat på antalet besökta länder
 * och uppdaterar statistics/globalStats dokumentet.
 */
// *** KORRIGERA SCHEMA IMPORT OCH SYNTAX ***
export const calculateTopUsersVisitedScheduled = onSchedule(
  {
    schedule: "0 */6 * * *",
    timeZone: "Europe/Stockholm",
  },
  async () => {
    functions.logger.info("Running scheduled job to calculate top users visited.");

    const db = firestoreDB;

    try {
      const usersQuerySnapshot = await db.collection("users")
        .orderBy("visitedCountriesCount", "desc")
        .limit(5)
        .select("username", "visitedCountriesCount")
        .get();

      const topUsersVisitedList = usersQuerySnapshot.docs.map((doc) => {
        const data = doc.data() as UserProfile;
        const username = data.username ?? "Unnamed User";
        const count = data.visitedCountriesCount ?? 0;

        return {
          userId: doc.id,
          username,
          count,
        };
      });

      const globalStatsDocRef = db.collection("statistics").doc("globalStats");

      await globalStatsDocRef.set({
        topUsersVisited: topUsersVisitedList,
        lastCalculated: admin.firestore.Timestamp.now(),
      }, {merge: true});

      functions.logger.info("Successfully calculated and updated top users visited list.");
    } catch (error) {
      functions.logger.error("Failed to calculate top users visited.", error);
      if (error instanceof Error) {
        functions.logger.error(`Error details: ${error.name} - ${error.message}`, error.stack);
      }
    }

    return;
  }
);
