package com.example.urban_safety.repositories

import com.example.urban_safety.data.model.EmergencyContact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseEmergencyContactsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : EmergencyContactsRepository {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    private val contactsCollection
        get() = firestore.collection("users")
            .document(currentUserId)
            .collection("emergency_contacts")

    override fun getEmergencyContacts(): Flow<List<EmergencyContact>> = callbackFlow {
        val registration = contactsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val contacts = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<EmergencyContact>()
            } ?: emptyList()

            trySend(contacts)
        }

        awaitClose { registration.remove() }
    }

    override suspend fun addEmergencyContact(contact: EmergencyContact) {
        contactsCollection.add(contact).await()
    }

    override suspend fun removeEmergencyContact(contactId: String) {
        contactsCollection.document(contactId).delete().await()
    }
} 