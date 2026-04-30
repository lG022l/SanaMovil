package com.g022.sanamovil.database

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

// Este "SupabaseHelper" es simplemente nuestra herramienta interna
// que toda tu app usará para hablar con la base de datos en la nube.
object SupabaseHelper {

    // Pega aquí los valores que copiaste de tu panel de Supabase
    private const val SUPABASE_URL = "https://yjlwrybylqwxxzbjqynd.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlqbHdyeWJ5bHF3eHh6YmpxeW5kIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzc0OTM0NDQsImV4cCI6MjA5MzA2OTQ0NH0.ZwGtP_h3YiDZuBicTt0Y-Nnbtd6WKB4DPlHw2Q8YmQY"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest) // Instala el módulo de Base de datos
        install(Auth)      // Instala el módulo de Login/Registro
    }
}