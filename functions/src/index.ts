// Uppdaterade imports för Functions V2
import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
// Importera typer för V2 Firestore triggers
import {Change, DocumentSnapshot, FirestoreEvent, DocumentOptions} from "firebase-functions/v2/firestore";
//import { getFirestore } from "@google-cloud/firestore";
// Initiera Firebase Admin SDK.
admin.initializeApp();

// *** LÖSNING FÖR 'databaseId' FELET ***
// Använd överlagringen som tar App som första argument
// const firestoreSettings: admin.firestore.Settings = { databaseId: "traveltracker" };
// Hämta standard-appen och skicka den tillsammans med inställningarna
// const firestoreDB = admin.firestore(admin.app(), firestoreSettings);
//const firestoreDB = getFirestore(admin.app(), "traveltracker");
const firestoreDB = admin.firestore();

// Vill du arbeta med en sekundär databas (icke-standard) i Admin SDK, används istället:
// const secondApp = admin.initializeApp({ projectId: 'my-project' }, 'secondApp');
// const db = getFirestore(secondApp); // Men kräver import från firestore lib direkt
// Men för din användning av traveltracker i triggern räcker standardinstansen.


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
// *** KORREKT V2 TRIGGER SYNTAX OCH SIGNATUR ***
export const aggregateUserCountryWrite = functions.firestore
// onDocumentWritten tar ETT OPTIONS OBJEKT som FÖRSTA argument, och HANDLER som ANDRA.
  .onDocumentWritten(
        // 1. Options (Object), inklusive document path OCH database ID
        {
          document: "/users/{userId}/userCountries/{countryCode}", // Path inuti options
          region: "europe-west3",
          database: "traveltracker", // Database ID inuti options
        } as DocumentOptions<string>, // Explicit cast kan fortfarande hjälpa kompilatorn

        // 2. Handler (Async function med event parameter) - Detta är nu det ANDRA argumentet
        async (event: FirestoreEvent<Change<DocumentSnapshot> | undefined>) => {
          const change = event.data; // Change objektet finns i event.data i V2
          if (!change) {
            functions.logger.error("No change data in event.");
            return null;
          }

          const countryCode = event.params.countryCode as string; // Sätt typen från params

          // Hämta data innan och efter ändringen
          const beforeSnapshot = change.before;
          const afterSnapshot = change.after;

          const beforeData = beforeSnapshot.exists ? (beforeSnapshot.data() as UserCountryData) : null;
          const afterData = afterSnapshot.exists ? (afterSnapshot.data() as UserCountryData) : null;

          // Om dokumentet raderades
          const isDeleted = !afterSnapshot.exists;

          // Beräkna status- och betygsändringar
          const oldStatus = beforeData?.status;
          const newStatus = afterData?.status;
          const oldRating = beforeData?.rating ?? null;
          const newRating = afterData?.rating ?? null;

          let visitedChange = 0;
          if (oldStatus !== "VISITED" && newStatus === "VISITED") {
            visitedChange = 1;
          } else if (oldStatus === "VISITED" && newStatus !== "VISITED" && newStatus !== undefined) {
            visitedChange = -1;
          } else if (oldStatus === "VISITED" && isDeleted) {
            visitedChange = -1;
          }

          let ratingSumChange = 0;
          let ratingCountChange = 0;

          if (oldRating == null && newRating != null) {
            ratingSumChange = newRating;
            ratingCountChange = 1;
          } else if (oldRating != null && newRating == null) {
            ratingSumChange = -oldRating;
            ratingCountChange = -1;
          } else if (oldRating != null && newRating != null && oldRating !== newRating) {
            ratingSumChange = newRating - oldRating;
            ratingCountChange = 0;
          } else if (oldRating != null && isDeleted) {
            ratingSumChange = -oldRating;
            ratingCountChange = -1;
          }

          if (visitedChange === 0 && ratingSumChange === 0 && ratingCountChange === 0) {
            functions.logger.log(`No relevant change for ${countryCode}. Skipping aggregation.`);
            return null;
          }

          // --- KÖR TRANSAKTION ---
          // *** ÄNDRA .document() TILL .doc() I ADMIN SDK ANROP ***
          const aggregateDocRef = firestoreDB.collection("countryAggregates").doc(countryCode);
          const globalStatsDocRef = firestoreDB.collection("statistics").doc("globalStats");

          try {
            await firestoreDB.runTransaction(async (transaction) => {
              // 1. Läs nuvarande aggregering för landet (inom transaktionen!)
              // *** Lägg till explicit cast här för att hjälpa kompilatorn med .data() ***
              const aggregateDoc = await transaction.get(aggregateDocRef) as admin.firestore.DocumentSnapshot;
              const currentAggregate = (aggregateDoc.data() as CountryAggregate | undefined) || {visitedCount: 0, ratingSum: 0, ratingCount: 0};

              // 2. Uppdatera aggregeringen
              currentAggregate.visitedCount += visitedChange;
              currentAggregate.ratingSum += ratingSumChange;
              currentAggregate.ratingCount += ratingCountChange;

              // Se till att räkningarna inte blir negativa
              currentAggregate.visitedCount = Math.max(0, currentAggregate.visitedCount);
              currentAggregate.ratingCount = Math.max(0, currentAggregate.ratingCount);
              if (currentAggregate.ratingCount === 0) {
                currentAggregate.ratingSum = 0;
              }

              // 3. Skriv tillbaka den uppdaterade aggregeringen för landet
              transaction.set(aggregateDocRef, currentAggregate);

              // --- BERÄKNA GLOBALA TOPPLISTOR ---
              // Läser alla dokument i collection "countryAggregates"
              const allAggregatesSnapshot = await transaction.get(firestoreDB.collection("countryAggregates")); // Returns QuerySnapshot
              const allAggregates = allAggregatesSnapshot.docs.map((doc) => ({countryCode: doc.id, ...doc.data() as CountryAggregate}));

              // Beräkna topp 5 mest besökta
              const mostVisited = allAggregates
                .filter((agg) => agg.visitedCount > 0)
                .sort((a, b) => b.visitedCount - a.visitedCount)
                .slice(0, 5)
                .map((agg) => ({countryCode: agg.countryCode, count: agg.visitedCount}));

              // Beräkna topp 5 högst betygsatta
              const highestRated = allAggregates
                .filter((agg) => agg.ratingCount > 0)
                .map((agg) => ({...agg, averageRating: agg.ratingSum / agg.ratingCount})) // Beräkna medelvärde
                .sort((a, b) => b.averageRating - a.averageRating)
                .slice(0, 5)
                .map((agg) => ({countryCode: agg.countryCode, averageRating: agg.averageRating}));

              // 4. Läs nuvarande globalStats dokument
              // *** Lägg till explicit cast här för att hjälpa kompilatorn med .data() ***
              const globalStatsDoc = await transaction.get(globalStatsDocRef) as admin.firestore.DocumentSnapshot;
              const currentGlobalStats = (globalStatsDoc.data() as GlobalStats | undefined) || {mostVisited: [], highestRated: [], lastCalculated: admin.firestore.Timestamp.now()};

              // 5. Uppdatera globalStats med de nya topplistorna
              currentGlobalStats.mostVisited = mostVisited;
              currentGlobalStats.highestRated = highestRated;
              currentGlobalStats.lastCalculated = admin.firestore.Timestamp.now();

              // 6. Skriv tillbaka det uppdaterade globalStats dokumentet
              transaction.set(globalStatsDocRef, currentGlobalStats);

              functions.logger.log(`Successfully updated aggregates for ${countryCode} and recalculated global stats.`);
            }); // Slut på transaktionen

            return null; // Returnera null vid lyckad körning
          } catch (error) {
            functions.logger.error(`Transaction failed for ${countryCode}:`, error);
            return null;
          }
        }
  );
