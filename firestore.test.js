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

test("Unauthenticated user: cannot read videos", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("videos").get());
});

test("Authenticated user: Alice can create and read her own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice",
    createdAt: new Date().toISOString()
  }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: Bob cannot update Alice's profile", async () => {
  const adminDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await adminDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice",
    createdAt: new Date().toISOString()
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({
    displayName: "Hacked Alice"
  }));
});

test("Video creation: Alice creates a video with her creatorId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("videos").doc("v_1").set({
    id: "v_1",
    title: "Alice First Video",
    videoUrl: "https://example.com/v.mp4",
    creatorId: ALICE_UID,
    status: "published",
    visibility: "public",
    viewsCount: 0,
    likesCount: 0
  }));
});

test("Video reading: Bob can read Alice's published public video", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("videos").doc("v_1").set({
    id: "v_1",
    title: "Alice First Video",
    videoUrl: "https://example.com/v.mp4",
    creatorId: ALICE_UID,
    status: "published",
    visibility: "public",
    viewsCount: 0,
    likesCount: 0
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("videos").doc("v_1").get());
});

test("Video deletion: Bob cannot delete Alice's video", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("videos").doc("v_1").set({
    id: "v_1",
    title: "Alice First Video",
    videoUrl: "https://example.com/v.mp4",
    creatorId: ALICE_UID,
    status: "published",
    visibility: "public"
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("videos").doc("v_1").delete());
});

test("Comments: Bob can post a comment to Alice's video", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("videos").doc("v_1").set({
    id: "v_1",
    title: "Alice First Video",
    videoUrl: "https://example.com/v.mp4",
    creatorId: ALICE_UID,
    status: "published",
    visibility: "public"
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("videos").doc("v_1").collection("comments").doc("c_1").set({
    id: "c_1",
    videoId: "v_1",
    userId: BOB_UID,
    userName: "Bob",
    text: "Great video Alice!",
    createdAt: new Date().toISOString()
  }));
});
