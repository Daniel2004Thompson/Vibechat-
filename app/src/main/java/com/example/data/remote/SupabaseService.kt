package com.example.data.remote

import android.content.Context
import com.example.util.HardwareVaultManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SupabaseApiService {

    // --- Supabase Auth Endpoints ---
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: SupabaseSignUpRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body request: SupabasePasswordSignInRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token")
    suspend fun signInWithIdToken(
        @Query("grant_type") grantType: String = "id_token",
        @Body request: SupabaseIdTokenSignInRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/otp")
    suspend fun sendEmailOtp(
        @Body request: SupabaseOtpRequest
    ): Response<Unit>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Body request: SupabaseRecoverRequest
    ): Response<Unit>

    @POST("auth/v1/verify")
    suspend fun verifyEmailOtp(
        @Body request: SupabaseVerifyOtpRequest
    ): Response<SupabaseAuthResponse>

    @retrofit2.http.PUT("auth/v1/user")
    suspend fun updateUser(
        @Header("Authorization") bearerToken: String,
        @Body request: Map<String, String>
    ): Response<SupabaseUserDto>

    @POST("auth/v1/token")
    suspend fun refreshAuthToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body request: Map<String, String>
    ): Response<SupabaseAuthResponse>

    // --- Supabase PostgREST Table Endpoints ---
    @GET("rest/v1/profiles")
    suspend fun getProfiles(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseProfileDto>>

    @GET("rest/v1/profiles")
    suspend fun getProfileByUsername(
        @Query("username") usernameFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseProfileDto>>

    @GET("rest/v1/profiles")
    suspend fun searchProfiles(
        @Query("or") orFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseProfileDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Body profile: SupabaseProfileDto
    ): Response<List<SupabaseProfileDto>>

    // --- Supabase public.public_profiles View Endpoints ---
    @GET("rest/v1/public_profiles")
    suspend fun getPublicProfiles(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseProfileDto>>

    @GET("rest/v1/public_profiles")
    suspend fun searchPublicProfiles(
        @Query("or") orFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseProfileDto>>

    @GET("rest/v1/public_profiles")
    suspend fun getPublicProfileByUsername(
        @Query("username") usernameFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseProfileDto>>

    // --- Supabase public.users Table Endpoints ---
    @GET("rest/v1/users")
    suspend fun getUsersTable(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseUserTableDto>>

    @GET("rest/v1/users")
    suspend fun getUserTableByUsername(
        @Query("username") usernameFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseUserTableDto>>

    @GET("rest/v1/users")
    suspend fun searchUsersTable(
        @Query("or") orFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseUserTableDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/users")
    suspend fun upsertUserTable(
        @Body user: SupabaseUserTableDto
    ): Response<List<SupabaseUserTableDto>>

    // --- Supabase RPC Direct Auth Users Search ---
    @POST("rest/v1/rpc/search_users")
    suspend fun searchUsersRpc(
        @Body body: Map<String, String>
    ): Response<List<SupabaseProfileDto>>

    @GET("rest/v1/conversations")
    suspend fun getConversations(
        @Query("select") select: String = "*",
        @Query("order") order: String = "last_message_timestamp.desc"
    ): Response<List<SupabaseConversationDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/conversations")
    suspend fun upsertConversation(
        @Body conversation: SupabaseConversationDto
    ): Response<List<SupabaseConversationDto>>

    @GET("rest/v1/messages")
    suspend fun getMessages(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at_millis.asc",
        @Query("conversation_id") conversationIdFilter: String? = null
    ): Response<List<SupabaseMessageDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/messages")
    suspend fun upsertMessage(
        @Body message: SupabaseMessageDto
    ): Response<List<SupabaseMessageDto>>

    @DELETE("rest/v1/messages")
    suspend fun deleteMessage(
        @Query("id") idFilter: String
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/messages")
    suspend fun updateMessageDeliveryStatus(
        @Query("id") idFilter: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/messages")
    suspend fun markConversationMessagesRead(
        @Query("conversation_id") convIdFilter: String,
        @Query("sender_id") senderIdFilter: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("rest/v1/statuses")
    suspend fun getStatuses(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at_millis.desc"
    ): Response<List<SupabaseStatusDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/statuses")
    suspend fun upsertStatus(
        @Body status: SupabaseStatusDto
    ): Response<List<SupabaseStatusDto>>

    @DELETE("rest/v1/statuses")
    suspend fun deleteStatus(
        @Query("id") idFilter: String,
        @Header("Authorization") bearerOverride: String? = null
    ): Response<Unit>
}

/**
 * Manages runtime connection through the user's Edge Function Access-Point:
 * https://dktdmacqwloszwkvprcy.supabase.co/functions/v1/Access-point
 * Acting as the main root to everything in the project.
 * ZERO API keys are compiled into or stored in the APK.
 */
object SupabaseClientManager {
    const val DEFAULT_ACCESS_POINT_URL = "https://dktdmacqwloszwkvprcy.supabase.co/functions/v1/Access-point"

    @Volatile
    var currentAccessToken: String? = null

    @Volatile
    private var runtimeEndpointUrl: String = DEFAULT_ACCESS_POINT_URL

    @Volatile
    private var cachedService: SupabaseApiService? = null

    init {
        cachedService = buildServiceOrNull()
    }

    val rawUrl: String
        get() = runtimeEndpointUrl

    fun getProjectBaseUrl(): String {
        val url = runtimeEndpointUrl
        return when {
            url.contains("/functions/") -> url.substringBefore("/functions/")
            url.contains("/rest/") -> url.substringBefore("/rest/")
            url.contains("/auth/") -> url.substringBefore("/auth/")
            else -> url.trimEnd('/')
        }
    }

    val isConfigured: Boolean
        get() = runtimeEndpointUrl.startsWith("https://") || runtimeEndpointUrl.startsWith("http://")

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun initializeFromVault(context: Context): HardwareVaultManager.VaultConfig {
        val config = HardwareVaultManager.loadVaultConfig(context)
        applyRuntimeConfig(
            endpointUrl = config.accessPointUrl.ifBlank { config.backendProxyUrl }.ifBlank { DEFAULT_ACCESS_POINT_URL }
        )
        return config
    }

    @Synchronized
    fun applyRuntimeConfig(
        endpointUrl: String
    ) {
        runtimeEndpointUrl = endpointUrl.trim().ifBlank { DEFAULT_ACCESS_POINT_URL }
        cachedService = buildServiceOrNull()
    }

    @Synchronized
    fun applyRuntimeConfig(
        supabaseUrl: String,
        supabaseKey: String,
        backendProxyUrl: String
    ) {
        val target = supabaseUrl.trim().ifBlank { backendProxyUrl.trim() }.ifBlank { DEFAULT_ACCESS_POINT_URL }
        applyRuntimeConfig(target)
    }

    val api: SupabaseApiService?
        get() = cachedService ?: synchronized(this) {
            cachedService ?: buildServiceOrNull().also { cachedService = it }
        }

    private fun buildServiceOrNull(): SupabaseApiService? {
        val activeEndpoint = runtimeEndpointUrl.ifBlank { DEFAULT_ACCESS_POINT_URL }
        if (!activeEndpoint.startsWith("http://") && !activeEndpoint.startsWith("https://")) {
            return null
        }

        val normalizedBaseUrl = if (activeEndpoint.endsWith("/")) activeEndpoint else "$activeEndpoint/"

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder()
                .header("Content-Type", "application/json")

            // Provide x-target-path header for the Edge Function access-point
            val fullUrl = originalRequest.url
            val pathStr = fullUrl.encodedPath
            val afterAccessPoint = pathStr.substringAfter("Access-point", "").trim()
            val targetPath = when {
                afterAccessPoint.startsWith("/") -> afterAccessPoint
                afterAccessPoint.isNotBlank() -> "/$afterAccessPoint"
                else -> "/"
            }
            val queryStr = fullUrl.query
            val fullTargetPath = if (queryStr.isNullOrBlank()) targetPath else "$targetPath?$queryStr"
            builder.header("x-target-path", fullTargetPath)

            // Only attach user's authenticated bearer token if logged in.
            // All API keys have been removed — the Edge Function handles backend auth.
            val bearer = currentAccessToken?.takeIf { it.isNotBlank() }
            if (bearer != null) {
                builder.header("Authorization", "Bearer $bearer")
            }
            chain.proceed(builder.build())
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return try {
            Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SupabaseApiService::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun parseSupabaseErrorMessage(errorJson: String?, fallback: String = "Authentication request failed"): String {
        if (errorJson.isNullOrBlank()) return fallback
        return try {
            val json = org.json.JSONObject(errorJson)
            val msg = json.optString("msg")
                .ifBlank { json.optString("message") }
                .ifBlank { json.optString("error_description") }
                .ifBlank { json.optString("error") }
            val result = if (msg.isNotBlank()) msg else fallback
            result.replace("supabase", "cloud", ignoreCase = true)
        } catch (_: Exception) {
            val raw = if (errorJson.length > 200) fallback else errorJson
            raw.replace("supabase", "cloud", ignoreCase = true)
        }
    }

    const val SUPABASE_SQL_SCHEMA = """
-- ============================================================
-- VibeChat: Complete Cloud Sync & Auth.Users Auto-Sync Script
-- Run this in your Supabase Project -> SQL Editor -> Run
-- ============================================================

-- 1. Create profiles table if not exists
CREATE TABLE IF NOT EXISTS public.profiles (
  id TEXT PRIMARY KEY,
  email TEXT NOT NULL,
  username TEXT NOT NULL,
  display_name TEXT NOT NULL,
  bio TEXT DEFAULT 'Hey there! I''m vibing on VibeChat ✨',
  avatar_color TEXT DEFAULT '#10B981',
  avatar_url TEXT,
  status_text TEXT DEFAULT 'Available',
  is_online BOOLEAN DEFAULT true,
  last_seen_millis BIGINT DEFAULT 0
);

-- 2. Ensure public.users table exists with matching columns
CREATE TABLE IF NOT EXISTS public.users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT NOT NULL,
  username TEXT,
  full_name TEXT,
  bio TEXT DEFAULT 'Hey there! I''m vibing on VibeChat ✨',
  avatar_url TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- 3. Conversations, Messages, and Statuses tables
CREATE TABLE IF NOT EXISTS public.conversations (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  is_group BOOLEAN DEFAULT false,
  description TEXT DEFAULT '',
  avatar_color TEXT DEFAULT '#10B981',
  participant_ids_csv TEXT DEFAULT '',
  admin_ids_csv TEXT DEFAULT '',
  last_message_text TEXT DEFAULT '',
  last_message_sender_name TEXT DEFAULT '',
  last_message_timestamp BIGINT DEFAULT 0,
  last_message_type TEXT DEFAULT 'TEXT'
);

CREATE TABLE IF NOT EXISTS public.messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
  sender_id TEXT NOT NULL,
  sender_name TEXT NOT NULL,
  sender_avatar_color TEXT DEFAULT '#10B981',
  content TEXT NOT NULL,
  message_type TEXT DEFAULT 'TEXT',
  media_uri TEXT,
  voice_duration_sec INT DEFAULT 0,
  waveform_csv TEXT DEFAULT '',
  reply_to_message_id TEXT,
  reply_to_sender_name TEXT,
  reply_to_preview TEXT,
  reactions_csv TEXT DEFAULT '',
  delivery_status TEXT DEFAULT 'SENT',
  created_at_millis BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.statuses (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  user_name TEXT NOT NULL,
  user_handle TEXT NOT NULL,
  user_avatar_color TEXT DEFAULT '#10B981',
  content TEXT NOT NULL,
  caption TEXT DEFAULT '',
  media_uri TEXT,
  background_color TEXT DEFAULT '#065F46',
  font_style TEXT DEFAULT 'DISPLAY',
  created_at_millis BIGINT DEFAULT 0,
  expires_at_millis BIGINT DEFAULT 0,
  viewer_ids_csv TEXT DEFAULT '',
  reactions_csv TEXT DEFAULT ''
);

-- 4. Enable Public Read & Write permissions (Open RLS Policies)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for profiles" ON public.profiles;
CREATE POLICY "Allow all for profiles" ON public.profiles FOR ALL USING (true) WITH CHECK (true);

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for users" ON public.users;
CREATE POLICY "Allow all for users" ON public.users FOR ALL USING (true) WITH CHECK (true);

ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for conversations" ON public.conversations;
CREATE POLICY "Allow all for conversations" ON public.conversations FOR ALL USING (true) WITH CHECK (true);

ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for messages" ON public.messages;
CREATE POLICY "Allow all for messages" ON public.messages FOR ALL USING (true) WITH CHECK (true);

ALTER TABLE public.statuses ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for statuses" ON public.statuses;
CREATE POLICY "Allow all for statuses" ON public.statuses FOR ALL USING (true) WITH CHECK (true);

GRANT ALL ON public.profiles TO anon, authenticated, service_role;
GRANT ALL ON public.users TO anon, authenticated, service_role;
GRANT ALL ON public.conversations TO anon, authenticated, service_role;
GRANT ALL ON public.messages TO anon, authenticated, service_role;
GRANT ALL ON public.statuses TO anon, authenticated, service_role;

-- 5. FUNCTION: Sync every user in auth.users into public.profiles & public.users
CREATE OR REPLACE FUNCTION public.handle_auth_user_sync()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_username text;
  v_display_name text;
BEGIN
  v_username := COALESCE(
    new.raw_user_meta_data->>'username',
    new.user_metadata->>'username',
    split_part(new.email, '@', 1)
  );
  v_display_name := COALESCE(
    new.raw_user_meta_data->>'display_name',
    new.raw_user_meta_data->>'full_name',
    new.user_metadata->>'display_name',
    new.user_metadata->>'full_name',
    v_username
  );

  -- Upsert into public.profiles
  INSERT INTO public.profiles (
    id, email, username, display_name, bio, avatar_color, is_online, last_seen_millis
  ) VALUES (
    new.id::text,
    new.email,
    v_username,
    v_display_name,
    COALESCE(new.raw_user_meta_data->>'bio', 'Hey there! I''m vibing on VibeChat ✨'),
    COALESCE(new.raw_user_meta_data->>'avatar_color', '#10B981'),
    true,
    (EXTRACT(EPOCH FROM now()) * 1000)::bigint
  )
  ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    display_name = EXCLUDED.display_name;

  -- Upsert into public.users
  INSERT INTO public.users (
    id, email, username, full_name, bio
  ) VALUES (
    new.id,
    new.email,
    v_username,
    v_display_name,
    COALESCE(new.raw_user_meta_data->>'bio', 'Hey there! I''m vibing on VibeChat ✨')
  )
  ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    full_name = EXCLUDED.full_name;

  RETURN new;
END;
$$;

-- 6. TRIGGER: Automatically fire whenever a user is created or updated in auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT OR UPDATE ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_auth_user_sync();

-- 7. BACKFILL: Retroactively copy all existing users from auth.users (including miebaka)
INSERT INTO public.profiles (
  id, email, username, display_name, bio, avatar_color, is_online, last_seen_millis
)
SELECT
  u.id::text,
  u.email,
  COALESCE(
    u.raw_user_meta_data->>'username',
    u.user_metadata->>'username',
    split_part(u.email, '@', 1)
  ) AS username,
  COALESCE(
    u.raw_user_meta_data->>'display_name',
    u.raw_user_meta_data->>'full_name',
    u.user_metadata->>'display_name',
    u.user_metadata->>'full_name',
    split_part(u.email, '@', 1)
  ) AS display_name,
  COALESCE(u.raw_user_meta_data->>'bio', 'Hey there! I''m vibing on VibeChat ✨') AS bio,
  COALESCE(u.raw_user_meta_data->>'avatar_color', '#10B981') AS avatar_color,
  true AS is_online,
  (EXTRACT(EPOCH FROM now()) * 1000)::bigint AS last_seen_millis
FROM auth.users u
ON CONFLICT (id) DO UPDATE SET
  email = EXCLUDED.email,
  username = EXCLUDED.username,
  display_name = EXCLUDED.display_name;

INSERT INTO public.users (
  id, email, username, full_name, bio
)
SELECT
  u.id,
  u.email,
  COALESCE(
    u.raw_user_meta_data->>'username',
    u.user_metadata->>'username',
    split_part(u.email, '@', 1)
  ) AS username,
  COALESCE(
    u.raw_user_meta_data->>'display_name',
    u.raw_user_meta_data->>'full_name',
    u.user_metadata->>'display_name',
    u.user_metadata->>'full_name',
    split_part(u.email, '@', 1)
  ) AS full_name,
  COALESCE(u.raw_user_meta_data->>'bio', 'Hey there! I''m vibing on VibeChat ✨') AS bio
FROM auth.users u
ON CONFLICT (id) DO UPDATE SET
  email = EXCLUDED.email,
  username = EXCLUDED.username,
  full_name = EXCLUDED.full_name;

-- 8. RPC FUNCTION: Direct search across auth.users
CREATE OR REPLACE FUNCTION public.search_users(search_term text)
RETURNS TABLE (
  id text,
  email text,
  username text,
  display_name text,
  bio text,
  avatar_color text,
  avatar_url text,
  status_text text,
  is_online boolean,
  last_seen_millis bigint
)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public, auth
AS $$
  SELECT
    u.id::text,
    u.email::text,
    COALESCE(
      u.raw_user_meta_data->>'username',
      u.user_metadata->>'username',
      split_part(u.email, '@', 1)
    )::text as username,
    COALESCE(
      u.raw_user_meta_data->>'display_name',
      u.raw_user_meta_data->>'full_name',
      u.user_metadata->>'display_name',
      u.user_metadata->>'full_name',
      split_part(u.email, '@', 1)
    )::text as display_name,
    COALESCE(u.raw_user_meta_data->>'bio', 'Hey there! I''m vibing on VibeChat ✨')::text as bio,
    COALESCE(u.raw_user_meta_data->>'avatar_color', '#10B981')::text as avatar_color,
    (u.raw_user_meta_data->>'avatar_url')::text as avatar_url,
    'Available'::text as status_text,
    true as is_online,
    (EXTRACT(EPOCH FROM now()) * 1000)::bigint as last_seen_millis
  FROM auth.users u
  WHERE
    search_term = '' OR
    u.email ILIKE '%' || search_term || '%' OR
    (u.raw_user_meta_data->>'username') ILIKE '%' || search_term || '%' OR
    (u.user_metadata->>'username') ILIKE '%' || search_term || '%' OR
    (u.raw_user_meta_data->>'display_name') ILIKE '%' || search_term || '%' OR
    (u.raw_user_meta_data->>'full_name') ILIKE '%' || search_term || '%';
$$;

GRANT EXECUTE ON FUNCTION public.search_users(text) TO anon, authenticated, service_role;
"""
}
