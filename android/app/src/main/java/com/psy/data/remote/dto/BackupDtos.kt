package com.psy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackupRequest(val blob: String)

@Serializable
data class BackupResponse(val version: Int, val blob: String, val updatedAt: String)
