package com.example.studenttracker

import com.example.studenttracker.data.network.*
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TrackerInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    @Before fun setup() {
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder().addInterceptor(TrackerInterceptor()).build()
        AuthSession.begin("test-google-token")
    }
    @After fun cleanup() { AuthSession.clear(); server.shutdown() }
    @Test fun movesReadParametersAndCredentialsIntoPostBody() {
        server.enqueue(MockResponse().setBody("""{"apiVersion":2,"status":"success","students":[]}"""))
        client.newCall(Request.Builder().url(server.url("/exec?action=getStudents&subjectCode=CS101")).build()).execute().use { assertTrue(it.isSuccessful) }
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/exec", request.path)
        val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals("test-google-token", body["idToken"].asString)
        assertEquals("getStudents", body["action"].asString)
        assertEquals("CS101", body["subjectCode"].asString)
    }
    @Test fun expiresSessionOnBackendAuthenticationFailure() {
        server.enqueue(MockResponse().setBody("""{"apiVersion":2,"status":"error","code":"UNAUTHENTICATED","message":"Sign in again"}"""))
        val exception = try { client.newCall(Request.Builder().url(server.url("/exec")).build()).execute(); null } catch (e: TrackerApiException) { e }
        assertEquals("UNAUTHENTICATED", exception?.code)
        assertNull(AuthSession.idToken)
    }
    @Test fun refusesOldBackendInsteadOfTrustingEmailOnlyRoles() {
        server.enqueue(MockResponse().setBody("""{"status":"success","role":"admin"}"""))
        val exception = try { client.newCall(Request.Builder().url(server.url("/exec")).build()).execute(); null } catch (e: TrackerApiException) { e }
        assertEquals("CONFIGURATION", exception?.code)
    }
    @Test fun reportsMalformedResponseWithoutCrashingOrAcceptingIt() {
        server.enqueue(MockResponse().setBody("<html>Deployment error</html>"))
        val exception = try { client.newCall(Request.Builder().url(server.url("/exec")).build()).execute(); null } catch (e: TrackerApiException) { e }
        assertEquals("INVALID_RESPONSE", exception?.code)
    }
    @Test fun makesNoNetworkRequestWithoutCredential() {
        AuthSession.clear()
        val exception = try { client.newCall(Request.Builder().url(server.url("/exec")).build()).execute(); null } catch (e: TrackerApiException) { e }
        assertEquals("UNAUTHENTICATED", exception?.code)
        assertEquals(0, server.requestCount)
    }
}
