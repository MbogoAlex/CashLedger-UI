package com.records.pesa.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://kldgevvjkizeixgriorv.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtsZGdldnZqa2l6ZWl4Z3Jpb3J2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MjU0MjUzMzYsImV4cCI6MjA0MTAwMTMzNn0.fDXdxNiX2rRlnypc2pfZ7Xr6_2ZkUjI845Ihmap2irw",
    ) {
        install(Auth)
        install(Storage)
        install(Postgrest)
    }
}