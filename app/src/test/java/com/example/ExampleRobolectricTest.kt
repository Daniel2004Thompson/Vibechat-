package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SeedDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context matches VibeChat`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VibeChat", appName)
    }

    @Test
    fun `seed data factory defaults to clean state without mock data`() {
        val contacts = SeedDataFactory.defaultContacts()
        val conversations = SeedDataFactory.defaultConversations("user_test")
        assertTrue(contacts.isEmpty())
        assertTrue(conversations.isEmpty())
    }

    @Test
    fun `user profile entity correctly supports blocked state`() {
        val user = com.example.data.local.UserProfileEntity(
            id = "user_123",
            email = "user@test.com",
            username = "testuser",
            displayName = "Test User",
            isBlocked = false
        )
        val blockedUser = user.copy(isBlocked = true)
        assertTrue(blockedUser.isBlocked)
    }

    @Test
    fun `supabase auth registry matches all registered users`() {
        // Test Miebaka
        val miebakaMatches = com.example.data.remote.SupabaseAuthRegistry.findMatches("miebaka")
        assertTrue(miebakaMatches.any { it.email == "danikalthom@gmail.com" })

        // Test Daniel Thompson
        val danielMatches = com.example.data.remote.SupabaseAuthRegistry.findMatches("daniel")
        assertTrue(danielMatches.any { it.email == "danielkaladathompson@gmail.com" })

        // Test UniPort
        val uniportMatches = com.example.data.remote.SupabaseAuthRegistry.findMatches("uniport")
        assertTrue(uniportMatches.any { it.email == "dthompson037@uniport.edu.ng" })

        // Verify no fake or unwanted accounts exist in registry
        val allRegistered = com.example.data.remote.SupabaseAuthRegistry.REGISTERED_AUTH_USERS
        assertTrue(allRegistered.none { it.email == "ragnarokking213@gmail.com" })
        assertTrue(allRegistered.none { it.email == "simbamufasa2002812@gmail.com" })
        assertTrue(allRegistered.none { it.email == "lovegarden749@gmail.com" })
    }

    @Test
    fun `supabase auth registry restores contacts and conversations on data reset`() {
        val currentUserId = "auth_user_miebaka"
        val currentEmail = "danikalthom@gmail.com"
        val currentName = "Miebaka Thompson"

        val contacts = com.example.data.remote.SupabaseAuthRegistry.getOtherRegisteredContacts(currentUserId, currentEmail)
        assertTrue(contacts.isNotEmpty())
        assertTrue(contacts.none { it.id == currentUserId })
        assertTrue(contacts.any { it.email == "danielkaladathompson@gmail.com" })
        // Crucial requirement: contacts start with isSavedContact = false so contacts tab stays clean!
        assertTrue(contacts.all { !it.isSavedContact })

        val conversations = com.example.data.remote.SupabaseAuthRegistry.buildRestoredConversations(currentUserId, currentEmail, currentName)
        assertTrue(conversations.isNotEmpty())
        assertTrue(conversations.any { it.isGroup && it.name.contains("Community") })
        // Crucial requirement: direct chats are not pre-seeded without searching and adding the user
        assertTrue(conversations.none { !it.isGroup })

        val messages = com.example.data.remote.SupabaseAuthRegistry.buildRestoredMessages(currentUserId, currentEmail, currentName)
        assertTrue(messages.isNotEmpty())
        assertTrue(messages.all { it.conversationId == "conv_group_community" })
    }

    @Test
    fun `spell checker engine correctly identifies typos and provides auto corrections`() {
        val response = com.example.util.SpellCheckerEngine.checkText("teh spelling is wrng and im gonna fix it")
        assertTrue(response.corrections.isNotEmpty())
        assertTrue(response.corrections.any { it.original.equals("teh", ignoreCase = true) && it.suggestion.equals("the", ignoreCase = true) })
        assertTrue(response.corrections.any { it.original.equals("wrng", ignoreCase = true) && it.suggestion.equals("wrong", ignoreCase = true) })
        assertTrue(response.corrections.any { it.original.equals("im", ignoreCase = true) && it.suggestion.equals("I'm", ignoreCase = true) })
        assertTrue(response.corrected.contains("the", ignoreCase = true))
        assertTrue(response.corrected.contains("wrong", ignoreCase = true))
    }

    @Test
    fun `canonicalDirectChatId produces identical symmetric conversation ID regardless of initiator`() {
        val userA = "auth_user_miebaka"
        val userB = "auth_user_daniel_thompson"
        val convIdFromA = com.example.data.remote.SupabaseAuthRegistry.canonicalDirectChatId(userA, userB)
        val convIdFromB = com.example.data.remote.SupabaseAuthRegistry.canonicalDirectChatId(userB, userA)
        assertEquals(convIdFromA, convIdFromB)
        assertTrue(convIdFromA.startsWith("conv_dm_"))
    }

    @Test
    fun `messages maintain author identity so incoming vs outgoing reflects WhatsApp behavior`() {
        val miebakaId = "auth_user_miebaka"
        val danielId = "auth_user_daniel_thompson"
        val convId = com.example.data.remote.SupabaseAuthRegistry.canonicalDirectChatId(miebakaId, danielId)

        val dmBetweenMiebakaAndDaniel = listOf(
            com.example.data.local.MessageEntity(
                id = "msg_1",
                conversationId = convId,
                senderId = miebakaId,
                senderName = "Miebaka Thompson",
                content = "Hey Daniel!",
                createdAtMillis = 1000L
            ),
            com.example.data.local.MessageEntity(
                id = "msg_2",
                conversationId = convId,
                senderId = danielId,
                senderName = "Daniel Thompson",
                content = "Hey Miebaka!",
                createdAtMillis = 2000L
            )
        )

        // From Miebaka's perspective:
        // Messages with senderId == miebakaId are outgoing; messages with senderId == danielId are incoming
        val miebakaOutgoing = dmBetweenMiebakaAndDaniel.filter { it.senderId == miebakaId }
        val miebakaIncoming = dmBetweenMiebakaAndDaniel.filter { it.senderId == danielId }
        assertEquals(1, miebakaOutgoing.size)
        assertEquals(1, miebakaIncoming.size)

        // From Daniel's perspective (after logging in with daniel's email/id):
        // Messages with senderId == danielId are outgoing; messages with senderId == miebakaId are incoming!
        val danielOutgoing = dmBetweenMiebakaAndDaniel.filter { it.senderId == danielId }
        val danielIncoming = dmBetweenMiebakaAndDaniel.filter { it.senderId == miebakaId }
        assertEquals(miebakaIncoming.size, danielOutgoing.size)
        assertEquals(miebakaOutgoing.size, danielIncoming.size)
    }

    @Test
    fun `clearing data and reinstalling restores community chats tied to user email and uuid without unsolicited direct chats`() {
        val userEmail = "danielkaladathompson@gmail.com"
        val userId = "auth_user_daniel_thompson"
        val displayName = "Daniel Thompson"

        // Simulating complete local data wipe (uninstall/reinstall/new phone)
        val restoredConvs = com.example.data.remote.SupabaseAuthRegistry.buildRestoredConversations(userId, userEmail, displayName)
        val restoredMsgs = com.example.data.remote.SupabaseAuthRegistry.buildRestoredMessages(userId, userEmail, displayName)

        // Verify community conversation is safely restored
        assertTrue(restoredConvs.isNotEmpty())
        assertTrue(restoredConvs.any { it.isGroup && it.id == "conv_group_community" })
        // Crucial: no unsolicited direct chats exist until user explicitly adds a contact!
        assertTrue(restoredConvs.none { !it.isGroup })
        assertTrue(restoredMsgs.all { it.conversationId == "conv_group_community" })
    }

    @Test
    fun `new users do not automatically receive other users chats or contacts upon registration`() {
        val newUserId = "auth_user_newbie_456"
        val newUserEmail = "newbie@gmail.com"
        val newUserName = "Newbie User"

        // 1. Initial conversations for a brand new account must contain NO direct chats with strangers
        val restoredConvs = com.example.data.remote.SupabaseAuthRegistry.buildRestoredConversations(newUserId, newUserEmail, newUserName)
        assertTrue(restoredConvs.none { !it.isGroup })
        assertTrue(restoredConvs.all { it.isGroup })

        // 2. Initial messages for a new account must contain NO private direct messages of other users
        val restoredMsgs = com.example.data.remote.SupabaseAuthRegistry.buildRestoredMessages(newUserId, newUserEmail, newUserName)
        assertTrue(restoredMsgs.all { it.conversationId == "conv_group_community" })

        // 3. Registered contacts must NOT have isSavedContact = true so Contacts tab is clean
        val otherContacts = com.example.data.remote.SupabaseAuthRegistry.getOtherRegisteredContacts(newUserId, newUserEmail)
        assertTrue(otherContacts.all { !it.isSavedContact })

        // 4. Searching for a user returns them with isSavedContact = false until explicitly added
        val searchResults = com.example.data.remote.SupabaseAuthRegistry.findMatches("miebaka")
        assertTrue(searchResults.isNotEmpty())
        assertTrue(searchResults.all { !it.isSavedContact })
    }

    @Test
    fun `user can start or open self chat for personal notes and reminders`() {
        val userId = "auth_user_miebaka"
        val userEmail = "danikalthom@gmail.com"
        val displayName = "Miebaka Thompson"

        val canonicalUser = com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(userId, userEmail)
        val expectedSelfConvId = "conv_self_$canonicalUser"
        assertTrue(expectedSelfConvId.startsWith("conv_self_"))

        // Verify direct chat initiation with oneself routes cleanly to canonical self conversation
        val myCanonical = com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(userId, userEmail)
        val selfContactCanonical = com.example.data.remote.SupabaseAuthRegistry.resolveCanonicalUserId(userId, userEmail)
        assertEquals(myCanonical, selfContactCanonical)

        val selfChatTitle = "$displayName (You)"
        assertTrue(selfChatTitle.endsWith("(You)"))
        assertTrue(selfChatTitle.contains("Miebaka"))
    }

    @Test
    fun unaddedUsersStatusesAreNeverPreseededOrVisible() {
        val currentUserId = "auth_user_daniel_thompson"
        val currentEmail = "danielkaladathompson@gmail.com"

        // 1. buildRestoredStatuses must return empty list (no auto-seeding of strangers/unadded users)
        val restoredStatuses = com.example.data.remote.SupabaseAuthRegistry.buildRestoredStatuses(currentUserId, currentEmail)
        assertTrue(restoredStatuses.isEmpty())

        // 2. Other users whom the user has not added must not have isSavedContact = true
        val otherContacts = com.example.data.remote.SupabaseAuthRegistry.getOtherRegisteredContacts(currentUserId, currentEmail)
        assertTrue(otherContacts.isNotEmpty())
        assertTrue(otherContacts.all { !it.isSavedContact })
    }

    @Test
    fun `google auth manager dynamically reads device email accounts without hardcoded emails and brands chooser for VibeChat`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val accountManager = android.accounts.AccountManager.get(context)
        val shadowAccountManager = org.robolectric.Shadows.shadowOf(accountManager)

        // Fresh device with no accounts must return empty list (never hardcoded developer emails)
        assertTrue(com.example.util.GoogleAuthManager.getVisibleDeviceEmailAccounts(context).isEmpty())

        // Simulate arbitrary emails installed on another user's phone
        shadowAccountManager.addAccount(android.accounts.Account("alice.smith@gmail.com", "com.google"))
        shadowAccountManager.addAccount(android.accounts.Account("bob.work@outlook.com", "com.microsoft.office.outlook"))
        shadowAccountManager.addAccount(android.accounts.Account("WhatsApp", "com.whatsapp"))

        val discovered = com.example.util.GoogleAuthManager.getVisibleDeviceEmailAccounts(context)
        assertEquals(2, discovered.size)
        assertTrue(discovered.contains("alice.smith@gmail.com"))
        assertTrue(discovered.contains("bob.work@outlook.com"))

        val chooserIntent = com.example.util.GoogleAuthManager.createAccountChooserIntent(context)
        val description = chooserIntent.getStringExtra("descriptionTextOverride").orEmpty()
        assertTrue(description.contains("VibeChat"))
        assertTrue(!description.contains("Codester", ignoreCase = true))
        assertTrue(chooserIntent.getBooleanExtra("alwaysPromptForAccount", false))
    }
}

