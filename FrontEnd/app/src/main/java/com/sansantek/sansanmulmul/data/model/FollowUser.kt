package com.sansantek.sansanmulmul.data.model

import com.google.gson.annotations.SerializedName

data class FollowUser(
    @SerializedName("userId") val userId: Int,
    @SerializedName("userProfileImg") val imgUrl: String?,
    @SerializedName("userBadge") val title: String?,
    @SerializedName("userNickName") val nickName: String?
)
