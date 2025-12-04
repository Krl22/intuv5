import {
  onCall,
  CallableRequest,
  HttpsError,
} from "firebase-functions/v2/https";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getDatabase } from "firebase-admin/database";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

initializeApp();

export const ping = onCall(
  { region: "us-central1" },
  (request: CallableRequest) => {
    const { name } = request.data as { name?: string };
    return { ok: true, message: "pong", name: name ?? null };
  }
);

export const acceptRide = onCall(
  { region: "us-central1" },
  async (request: CallableRequest) => {
    const driverUid = request.auth?.uid;
    const { rideRequestId } = request.data as { rideRequestId?: string };
    if (!driverUid) throw new HttpsError("unauthenticated", "Auth requerida");
    if (!rideRequestId)
      throw new HttpsError("invalid-argument", "rideRequestId requerido");

    const db = getDatabase();
    const fs = getFirestore();

    const reqRef = db.ref(`rideRequests/${rideRequestId}`);
    const statusRef = reqRef.child("status");
    const txn = await statusRef.transaction((cur) => {
      if (cur === "searching") return "assigned";
      return cur;
    });
    if (!txn.committed || txn.snapshot.val() !== "assigned") {
      throw new HttpsError(
        "failed-precondition",
        "Ride request no está disponible"
      );
    }
    const reqSnap = await reqRef.get();
    if (!reqSnap.exists())
      throw new HttpsError("not-found", "Ride request no existe");
    const req = reqSnap.val() as any;

    const userId: string | undefined = req.userId;
    const paymentMethod: string | undefined = req.paymentMethod;
    const rideType: string | undefined = req.rideType;
    const price: number | undefined = req.price;
    const originLat: number | undefined = req.originLat;
    const originLon: number | undefined = req.originLon;
    const destLat: number | undefined = req.destLat;
    const destLon: number | undefined = req.destLon;

    if (!userId)
      throw new HttpsError(
        "invalid-argument",
        "userId faltante en ride request"
      );

    const driverDoc = await fs.collection("users").doc(driverUid).get();
    if (!driverDoc.exists)
      throw new HttpsError(
        "failed-precondition",
        "Perfil de conductor no encontrado"
      );
    const d = driverDoc.data() || {};
    const driverName = (d.firstName as string) || "";
    const driverPhoto = (d.photoUrl as string) || undefined;
    const driverObj = (d.driver as Record<string, any>) || {};
    const vehicleType =
      (d.vehicleType as string) || (driverObj?.vehicleType as string) || "";
    const vehiclePlate =
      (d.vehiclePlate as string) || (driverObj?.vehiclePlate as string) || "";
    const vehiclePhoto =
      (
        (driverObj?.vehiclePhotoUrl as string) ||
        (d as any)?.vehiclePhotoUrl ||
        ""
      ).trim() || undefined;

    const userDoc = await fs.collection("users").doc(userId).get();
    const u = userDoc.exists ? userDoc.data() || {} : {};
    const clientFirst = (u.firstName as string) || "";
    const clientLast = (u.lastName as string) || "";
    const clientName =
      [clientFirst, clientLast].join(" ").trim() || clientFirst || "";
    const clientPhoto = (u.photoUrl as string) || undefined;

    const startCode = Math.floor(1000 + Math.random() * 9000);
    const data: Record<string, any> = {
      userId,
      driverId: driverUid,
      status: "active",
      startCode,
      paymentMethod: paymentMethod ?? "efectivo",
      rideType: rideType ?? "Intu Honda",
      price: price ?? 0,
      originLat: originLat ?? 0,
      originLon: originLon ?? 0,
      destLat: destLat ?? 0,
      destLon: destLon ?? 0,
      createdAt: { ".sv": "timestamp" },
      driverName,
      vehicleType,
      vehiclePlate,
    };
    if (driverPhoto) data["driverPhoto"] = driverPhoto;
    if (clientName) data["clientName"] = clientName;
    if (clientPhoto) data["clientPhoto"] = clientPhoto;
    if (vehiclePhoto) data["vehiclePhoto"] = vehiclePhoto;

    await db.ref(`currentRides/${rideRequestId}`).set(data);
    await db.ref(`rideRequests/${rideRequestId}`).remove();
    await db.ref(`driverAvailability/${driverUid}`).remove();

    return { ok: true, currentRideId: rideRequestId };
  }
);

export const processTopup = onDocumentUpdated(
  "topups/{topupId}",
  async (event) => {
    if (!event.data) return;
    const data = event.data!;
    const after = data.after.data() as any;
    const before = data.before.data() as any;
    const nowApproved = after?.isapproved === true;
    const wasApproved = before?.isapproved === true;
    const alreadyProcessed = after?.processed === true;
    if (!nowApproved || alreadyProcessed || wasApproved) return;
    const amount = Number(after?.amount ?? 0);
    const userId = after?.userId as string | undefined;
    if (!userId || !(amount > 0)) return;
    const fs = getFirestore();
    await fs.runTransaction(async (tx) => {
      const userRef = fs.collection("users").doc(userId);
      tx.update(userRef, { balance: FieldValue.increment(amount) });
      tx.update(data.after.ref, {
        processed: true,
        approvedAt: FieldValue.serverTimestamp(),
      });
      return null;
    });
  }
);

export const cancelRide = onCall(
  { region: "us-central1" },
  async (request: CallableRequest) => {
    const uid = request.auth?.uid;
    const { currentRideId } = request.data as { currentRideId?: string };
    if (!uid) throw new HttpsError("unauthenticated", "Auth requerida");
    if (!currentRideId)
      throw new HttpsError("invalid-argument", "currentRideId requerido");
    const db = getDatabase();
    const snap = await db.ref(`currentRides/${currentRideId}`).get();
    if (!snap.exists())
      throw new HttpsError("not-found", "Current ride no existe");
    const cur = snap.val() as any;
    const driverId = cur?.driverId as string | undefined;
    const userId = cur?.userId as string | undefined;
    if (uid !== driverId && uid !== userId)
      throw new HttpsError("permission-denied", "No autorizado");
    await db.ref(`currentRides/${currentRideId}/chat`).remove();
    await db.ref(`currentRides/${currentRideId}`).remove();
    if (driverId) await db.ref(`driverAvailability/${driverId}`).remove();
    return { ok: true };
  }
);

export const completeRide = onCall(
  { region: "us-central1" },
  async (request: CallableRequest) => {
    const uid = request.auth?.uid;
    const { currentRideId, finalPrice } = request.data as {
      currentRideId?: string;
      finalPrice?: number;
    };
    if (!uid) throw new HttpsError("unauthenticated", "Auth requerida");
    if (!currentRideId)
      throw new HttpsError("invalid-argument", "currentRideId requerido");
    const db = getDatabase();
    const fs = getFirestore();
    console.log("completeRide:start", { currentRideId, uid });
    const snap = await db.ref(`currentRides/${currentRideId}`).get();
    if (!snap.exists())
      throw new HttpsError("not-found", "Current ride no existe");
    const cur = snap.val() as any;
    const driverId = cur?.driverId as string | undefined;
    if (!driverId || uid !== driverId)
      throw new HttpsError(
        "permission-denied",
        "Solo el conductor puede completar"
      );
    const price =
      typeof finalPrice === "number" && finalPrice > 0
        ? finalPrice
        : Number(cur?.price ?? 0);
    await db.ref(`currentRides/${currentRideId}`).update({
      status: "completed",
      completedAt: { ".sv": "timestamp" },
      finalPrice: price,
    });
    console.log("completeRide:statusUpdated", { currentRideId, price });
    // Persist trip history in Firestore for both user and driver
    const userId = (cur?.userId as string | undefined) || undefined;
    const originLat = Number(cur?.originLat ?? 0);
    const originLon = Number(cur?.originLon ?? 0);
    const destLat = Number(cur?.destLat ?? 0);
    const destLon = Number(cur?.destLon ?? 0);
    const paymentMethod =
      (cur?.paymentMethod as string | undefined) || "efectivo";
    const rideType = (cur?.rideType as string | undefined) || "Intu Honda";
    const driverName = (cur?.driverName as string | undefined) || undefined;
    const clientName = (cur?.clientName as string | undefined) || undefined;
    const createdAt = cur?.createdAt ?? undefined;
    const verifiedAt = cur?.verifiedAt ?? undefined;
    const arrivedAt = cur?.arrivedAt ?? undefined;
    const tripData: Record<string, any> = {
      rideId: currentRideId,
      status: "completed",
      paymentMethod,
      rideType,
      price,
      originLat,
      originLon,
      destLat,
      destLon,
      createdAt: createdAt ?? FieldValue.serverTimestamp(),
      completedAt: FieldValue.serverTimestamp(),
    };
    if (userId) tripData["userId"] = userId;
    if (driverId) tripData["driverId"] = driverId;
    if (verifiedAt != null) tripData["verifiedAt"] = verifiedAt;
    if (arrivedAt != null) tripData["arrivedAt"] = arrivedAt;
    if (driverName) tripData["driverName"] = driverName;
    if (clientName) tripData["clientName"] = clientName;
    const writes: Promise<any>[] = [];
    if (userId) {
      writes.push(
        fs.collection("users").doc(userId).collection("trips").add(tripData)
      );
    }
    if (driverId) {
      writes.push(
        fs
          .collection("users")
          .doc(driverId)
          .collection("services")
          .add(tripData)
      );
    }
    const results = await Promise.all(writes);
    console.log("completeRide:tripsWritten", {
      currentRideId,
      userTrip: Boolean(userId),
      driverTrip: Boolean(driverId),
      resultsCount: results.length,
    });
    // Clean RTDB nodes
    await db.ref(`currentRides/${currentRideId}/chat`).remove();
    await db.ref(`currentRides/${currentRideId}`).remove();
    if (driverId) await db.ref(`driverAvailability/${driverId}`).remove();
    console.log("completeRide:currentRideDeleted", { currentRideId });
    return {
      ok: true,
      userTrip: Boolean(userId),
      driverTrip: Boolean(driverId),
      userId: userId ?? null,
      driverId: driverId ?? null,
    };
  }
);

export const driverArrived = onCall(
  { region: "us-central1" },
  async (request: CallableRequest) => {
    const uid = request.auth?.uid;
    const { currentRideId } = request.data as { currentRideId?: string };
    if (!uid) throw new HttpsError("unauthenticated", "Auth requerida");
    if (!currentRideId)
      throw new HttpsError("invalid-argument", "currentRideId requerido");
    const db = getDatabase();
    const snap = await db.ref(`currentRides/${currentRideId}`).get();
    if (!snap.exists())
      throw new HttpsError("not-found", "Current ride no existe");
    const cur = snap.val() as any;
    const driverId = cur?.driverId as string | undefined;
    if (!driverId || uid !== driverId)
      throw new HttpsError(
        "permission-denied",
        "Solo el conductor puede marcar llegada"
      );
    await db
      .ref(`currentRides/${currentRideId}`)
      .update({ status: "arrived", arrivedAt: { ".sv": "timestamp" } });
    return { ok: true };
  }
);

export const verifyStartCode = onCall(
  { region: "us-central1" },
  async (request: CallableRequest) => {
    const uid = request.auth?.uid;
    const { currentRideId, code } = request.data as {
      currentRideId?: string;
      code?: number;
    };
    if (!uid) throw new HttpsError("unauthenticated", "Auth requerida");
    if (!currentRideId || typeof code !== "number")
      throw new HttpsError("invalid-argument", "Parámetros inválidos");
    const db = getDatabase();
    const ref = db.ref(`currentRides/${currentRideId}`);
    const snap = await ref.get();
    if (!snap.exists())
      throw new HttpsError("not-found", "Current ride no existe");
    const cur = snap.val() as any;
    const driverId = cur?.driverId as string | undefined;
    const status = cur?.status as string | undefined;
    if (!driverId || uid !== driverId)
      throw new HttpsError("permission-denied", "No autorizado");
    if (status !== "arrived" && status !== "active")
      throw new HttpsError(
        "failed-precondition",
        "Estado inválido para validación"
      );
    const startCode = Number(cur?.startCode ?? 0);
    if (!(startCode >= 1000 && startCode <= 9999))
      throw new HttpsError("internal", "Código no disponible");
    const attempts = Number(cur?.wrongAttempts ?? 0);
    if (attempts >= 5)
      throw new HttpsError("resource-exhausted", "Demasiados intentos");
    if (code !== startCode) {
      await ref.update({ wrongAttempts: attempts + 1 });
      throw new HttpsError("failed-precondition", "Código incorrecto");
    }
    await ref.update({
      status: "in_progress",
      verifiedAt: { ".sv": "timestamp" },
      wrongAttempts: 0,
    });
    return { ok: true };
  }
);

export const submitRating = onCall(
  { region: "us-central1" },
  async (request: CallableRequest) => {
    const raterId = request.auth?.uid;
    const { rideId, targetUserId, role, stars, comment } = request.data as {
      rideId?: string;
      targetUserId?: string;
      role?: string;
      stars?: number;
      comment?: string;
    };
    if (!raterId) throw new HttpsError("unauthenticated", "Auth requerida");
    if (!rideId || !targetUserId)
      throw new HttpsError("invalid-argument", "Parámetros requeridos");
    const s = Number(stars);
    if (!(s >= 1 && s <= 5))
      throw new HttpsError("invalid-argument", "Stars inválidas");
    const fs = getFirestore();
    let valid = false;
    if (role === "driver") {
      const qs = await fs
        .collection("users")
        .doc(raterId)
        .collection("trips")
        .where("rideId", "==", rideId)
        .limit(1)
        .get();
      valid = !qs.empty;
    } else if (role === "passenger") {
      const qs = await fs
        .collection("users")
        .doc(raterId)
        .collection("services")
        .where("rideId", "==", rideId)
        .limit(1)
        .get();
      valid = !qs.empty;
    } else {
      throw new HttpsError("invalid-argument", "Role inválido");
    }
    if (!valid)
      throw new HttpsError(
        "failed-precondition",
        "Viaje no válido para rating"
      );
    const targetRef = fs.collection("users").doc(targetUserId);
    await fs.runTransaction(async (tx) => {
      const snap = await tx.get(targetRef);
      const data = snap.exists ? snap.data() || {} : {};
      const key = role === "driver" ? "driverRatings" : "passengerRatings";
      const sumKey = role === "driver" ? "driverRating" : "passengerRating";
      const existing = ((data[key] as Record<string, any>) || {})[rideId];
      if (existing) throw new HttpsError("already-exists", "Rating duplicado");
      const now = FieldValue.serverTimestamp();
      const entry: any = { rideId, raterId, stars: s, createdAt: now };
      if (
        role === "driver" &&
        typeof comment === "string" &&
        comment.trim().length > 0
      ) {
        entry["comment"] = comment.trim();
      }
      const updates: Record<string, any> = {};
      updates[`${key}.${rideId}`] = entry;
      const summary = (data[sumKey] as any) || {};
      const prevCount = Number(summary?.count ?? 0);
      const prevSum = Number(summary?.sum ?? 0);
      const newCount = prevCount + 1;
      const newSum = prevSum + s;
      const newAvg = newSum / newCount;
      updates[sumKey] = { count: newCount, sum: newSum, average: newAvg };
      tx.set(targetRef, updates, { merge: true });
      return null;
    });
    return { ok: true };
  }
);
