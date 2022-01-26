package org.csc.kotlin2021.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.csc.kotlin2021.HttpApi
import org.csc.kotlin2021.Message
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

class HttpChatClient(host: String, port: Int) {
    private val objectMapper = jacksonObjectMapper()
    private val httpApi: HttpApi = Retrofit.Builder()
        .baseUrl("http://$host:$port")
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .build().create(HttpApi::class.java)

    fun sendMessage(message: Message) {
        val response = httpApi.sendMessage(message).execute()
        if (!response.isSuccessful) {
            println("${response.code()} ${response.message()}}")
        }
    }
}
