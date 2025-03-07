package com.sansantek.sansanmulmul.data.network.api

import com.sansantek.sansanmulmul.data.model.FollowUser
import com.sansantek.sansanmulmul.data.model.KakaoLoginToken
import com.sansantek.sansanmulmul.data.model.KakaoLoginUser
import com.sansantek.sansanmulmul.data.model.MyPageInfo
import com.sansantek.sansanmulmul.data.model.ProfileUpdateData
import com.sansantek.sansanmulmul.data.model.User
import com.sansantek.sansanmulmul.data.model.UserToken
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {
    @Multipart
    @POST("user/signup")
    suspend fun registerUser(@Part image: MultipartBody.Part, @Part("SignUpUserRequest") kakaoLoginUser: KakaoLoginUser): Response<UserToken>
    @GET("user/login/{userProviderId}")
    suspend fun loginUser(@Path("userProviderId") id: String) : Response<KakaoLoginToken>
    @GET("user/nickname")
    suspend fun isAvailableNickName(@Query("userNickname") nickName: String) : Response<Boolean>
    @GET("user/info")
    suspend fun loadUserProfile(@Header("Authorization") accessToken: String) : Response<User>
    @GET("user/token")
    suspend fun refreshToken(@Header("Authorization") accessToken: String) : Response<KakaoLoginToken>
    @GET("user/style")
    suspend fun getHikingStyle(@Header("Authorization") accessToken: String): List<Int>
    @GET("user/followings")
    suspend fun getUserFollowing(@Header("Authorization") accessToken: String): List<FollowUser>
    @GET("user/followers")
    suspend fun getUserFollower(@Header("Authorization") accessToken: String): List<FollowUser>
    @GET("user/mypage")
    suspend fun getMyPageInfo(@Header("Authorization") accessToken: String): MyPageInfo
    @GET("user/badge")
    suspend fun getMyBadgeList(@Header("Authorization") accessToken: String) : List<String>
    @Multipart
    @PATCH("user/info")
    suspend fun updateUserProfile(@Header("Authorization") accessToken: String, @Part image: MultipartBody.Part, @Part("updateUserRequest") profileUpdateData: ProfileUpdateData) : Response<Boolean>
    @GET("user/chknick")
    suspend fun chkDuplicateNickname(@Header("Authorization") accessToken: String, @Query("userNickname") nickName: String): Response<Boolean>

    // 다른 멤버 정보 조회
    @GET("user/{userId}/info")
    suspend fun getMemberInfo(@Path("userId") id: Int) : Response<User>

    // 다른 멤버 팔로우 추가하기. userId는 내 id, followUserId는 팔로우할 유저 id
    @POST("user/{userId}/follow")
    suspend fun addMemberFollow(@Header("Authorization") accessToken: String, @Path("userId") id: Int, @Query("followUserId") followUserId: Int) : Response<String>

    // 다른 멤버의 팔로잉 조회
    @GET("user/{userId}/followings")
    suspend fun getMemberFollowing(@Path("userId") id : Int) : List<FollowUser>

    // 다른 멤버의 팔로워 조회
    @GET("user/{userId}/followers")
    suspend fun getMemberFollower(@Path("userId") id : Int) : List<FollowUser>

    // 다른 멤버 팔로우 취소. unfollowUserId는 언팔로우할 유저의 id
    @DELETE("user/unfollow")
    suspend fun deleteMemberFollow(@Header("Authorization") accessToken: String, @Query("unfollowUserId") unfollowUserId: Int) : Response<String>
}