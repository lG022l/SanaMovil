package com.g022.sanamovil.engine

interface LlamaStreamCallback {
    fun onToken(token: String)
}