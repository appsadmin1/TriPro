// Shared Firebase Admin SDK setup. Credentials come from Netlify environment variables
// (Site settings > Environment variables), never from a committed file — see the
// service account fields pulled from the JSON key you download in Firebase console >
// Project settings > Service accounts > Generate new private key. See README
// "Push notifications setup" for exactly which three fields to copy where.
//
// Firestore/Auth access via the Admin SDK does NOT require the Firebase project to be
// on the Blaze plan — Blaze is only required to *host compute* on Firebase itself
// (Cloud Functions, Cloud Run, App Hosting). Reading/writing Firestore and verifying ID
// tokens from an external Node process (like this one, running on Netlify) works fine
// on Spark, which is the whole point of this setup.

import { initializeApp, getApps, cert } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

function ensureInitialized() {
  if (getApps().length > 0) return;

  const projectId = process.env.FIREBASE_PROJECT_ID;
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  // Netlify's env var UI collapses real newlines, so the private key is stored with
  // literal "\n" sequences and unescaped here.
  const privateKey = (process.env.FIREBASE_PRIVATE_KEY || "").replace(/\\n/g, "\n");

  if (!projectId || !clientEmail || !privateKey) {
    throw new Error(
      "Missing FIREBASE_PROJECT_ID / FIREBASE_CLIENT_EMAIL / FIREBASE_PRIVATE_KEY env vars"
    );
  }

  initializeApp({
    credential: cert({ projectId, clientEmail, privateKey }),
  });
}

export function getAdminAuth() {
  ensureInitialized();
  return getAuth();
}

export function getAdminFirestore() {
  ensureInitialized();
  return getFirestore();
}

export function getAdminMessaging() {
  ensureInitialized();
  return getMessaging();
}
