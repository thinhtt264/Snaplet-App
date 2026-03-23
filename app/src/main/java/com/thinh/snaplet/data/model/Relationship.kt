package com.thinh.snaplet.data.model

import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.model.user.AvatarUrls

/**
 * Relationship status: pending | accepted | blocked
 */
enum class RelationshipStatus(val value: String) {
    @SerializedName("pending")
    PENDING("pending"),

    @SerializedName("accepted")
    ACCEPTED("accepted"),

    @SerializedName("blocked")
    BLOCKED("blocked");

    companion object {
        fun from(value: String): RelationshipStatus? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}

data class UpdateRelationshipRequest(
    @SerializedName("status")
    val status: String
)

data class Relationship(
    @SerializedName("id")
    val id: String,

    @SerializedName("user1Id")
    val user1Id: String,

    @SerializedName("user2Id")
    val user2Id: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("initiator")
    val initiator: String,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)

data class RelationshipCounts(
    @SerializedName("acceptedFriendCount")
    val acceptedFriendCount: Int,
    @SerializedName("pendingRequestCount")
    val pendingRequestCount: Int,
)

data class RelationshipWithUserDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,

    @SerializedName("avatarUrls")
    val avatarUrls: AvatarUrls = AvatarUrls(),

    @SerializedName("status")
    val status: String,

    @SerializedName("createdAt")
    val createdAt: String
) {
    fun toDomain(): RelationshipWithUser {
        val statusEnum = RelationshipStatus.from(status)
            ?: throw IllegalArgumentException("Unknown relationship status: $status")
        return RelationshipWithUser(
            id = id,
            userId = userId,
            username = username,
            firstName = firstName,
            lastName = lastName,
            avatarUrls = avatarUrls,
            status = statusEnum,
            createdAt = createdAt
        )
    }
}

data class RelationshipWithUser(
    val id: String,
    val userId: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrls: AvatarUrls,
    val status: RelationshipStatus,
    val createdAt: String
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { username }
}
