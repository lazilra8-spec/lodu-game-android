const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated checks
test("Unauthenticated user: cannot read user profiles or games", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("games").doc("game_1").get());
});

// 2. User profile operations
test("Authenticated user: can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceKing",
      countryCode: "US",
      flagEmoji: "🇺🇸",
      serverRegion: "us-east",
      avatarIndex: 0,
      gamesPlayed: 0,
      gamesWon: 0,
      rankPoints: 1000,
      createdAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Cross-user: Bob cannot update Alice's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    username: "AliceKing",
    countryCode: "US",
    flagEmoji: "🇺🇸",
    serverRegion: "us-east",
    avatarIndex: 0,
    gamesPlayed: 0,
    gamesWon: 0,
    rankPoints: 1000,
    createdAt: now,
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      username: "HackedName",
      updatedAt: new Date(),
    })
  );
});

// 3. Game Room operations
test("Authenticated user: can create a game room where they are host", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("games").doc("game_123").set({
      gameId: "game_123",
      serverRegion: "us-east",
      mode: "4_PLAYER",
      isPrivate: false,
      roomCode: "123456",
      status: "waiting",
      hostId: ALICE_UID,
      playerIds: [ALICE_UID],
      createdAt: now,
    })
  );
});

test("Non-participant cannot move or update game once playing", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("games").doc("game_playing").set({
      gameId: "game_playing",
      serverRegion: "us-east",
      mode: "4_PLAYER",
      isPrivate: false,
      status: "playing",
      hostId: ALICE_UID,
      playerIds: [ALICE_UID],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("games").doc("game_playing").update({
      diceValue: 6,
      updatedAt: new Date(),
    })
  );
});

test("In-game chat: Participant can send message but cannot edit or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const msgDoc = aliceDb.collection("games").doc("game_123").collection("messages").doc("msg_1");
  const now = new Date();

  await assertSucceeds(
    msgDoc.set({
      gameId: "game_123",
      senderId: ALICE_UID,
      senderName: "AliceKing",
      senderColor: "RED",
      message: "Good luck everyone!",
      createdAt: now,
    })
  );

  await assertFails(
    msgDoc.update({
      message: "Edited message",
    })
  );

  await assertFails(msgDoc.delete());
});
