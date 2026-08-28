package com.example.sahamscraper.api

import com.example.sahamscraper.data.Prices
import retrofit2.http.Body
import retrofit2.http.POST

interface WorkerApi {
    @POST("/")
    suspend fun sendPrices(@Body prices: Prices): retrofit2.Response<String>
}
