import { onCall, CallableRequest } from "firebase-functions/v2/https";
import { initializeApp } from "firebase-admin/app";

initializeApp();

export const ping = onCall((request: CallableRequest) => {
  const { name } = request.data as { name?: string };
  return { ok: true, message: "pong", name: name ?? null };
});
