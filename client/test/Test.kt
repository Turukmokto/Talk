package org.csc.kotlin2021

import org.csc.kotlin2021.client.HttpChatClient
import org.csc.kotlin2021.server.HttpChatServer
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import org.csc.kotlin2021.server.ChatMessageListener

class TestChat : ChatMessageListener {
    var messageReceivedFlag: Boolean = false
    var fromUserName: String ? = null
    var text: String ? = null

    override fun messageReceived(userName: String, msg: String) {
        messageReceivedFlag = true
        fromUserName = userName
        text = msg
    }
}

class Tests {
    data class User(
        private val toHost: String,
        private val myPort: Int,
        private val toPort: Int,
        val userName: String,
        val msg: String
    ) {
        private val chatClient = HttpChatClient(toHost, toPort)
        private val server = HttpChatServer(toHost, myPort)
        private val chat = TestChat()
        private val serverJob = thread { server.start() }

        init {
            server.setMessageListener(chat)
        }

        fun sendMessage() {
            chatClient.sendMessage(Message(userName, msg))
        }

        fun checkReceived(fromName: String, msg: String) {
            with(chat) {
                assertEquals(true, messageReceivedFlag)
                assertEquals(fromName, fromUserName)
                assertEquals(msg, text)
            }
        }

        fun stopAll() {
            server.stop()
            serverJob.stop()
        }
    }

    @Test
    fun test1() {
        val host = "localhost"
        val port = 8080

        with(User(host, port, port, "phoenix", "Hello")) {
            sendMessage()
            checkReceived(userName, msg)
            stopAll()
        }
    }

    @Test
    fun dualTest() {
        val host = "localhost"
        val port1 = 8080
        val port2 = 8081

        val user1 = User(host, port1, port2, "phoenix1", "Hello1")
        val user2 = User(host, port2, port1, "phoenix2", "Hello2")

        user1.sendMessage()
        user2.checkReceived(user1.userName, user1.msg)

        user2.sendMessage()
        user1.checkReceived(user2.userName, user2.msg)

        user1.stopAll()
        user2.stopAll()
    }
}
