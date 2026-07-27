package com.tripro.app.data.model

/**
 * Permission level a user has on a trip. Stored as the raw string value inside
 * Trip.members (Firestore map<userId, roleString>) so it round-trips without a
 * custom Firestore type adapter.
 */
enum class Role(val value: String) {
    OWNER("owner"),
    EDITOR("editor"),
    VIEWER("viewer");

    companion object {
        fun fromValue(value: String?): Role =
            entries.firstOrNull { it.value == value } ?: VIEWER
    }
}

/** True if this role is allowed to create/edit/delete itinerary content. */
fun Role.canEdit(): Boolean = this == Role.OWNER || this == Role.EDITOR
