package com.beachguard.projeto3_equipe26.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.beachguard.projeto3_equipe26.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class MapsMarkerAdapter (private val context: Context) : GoogleMap.InfoWindowAdapter{
    // Classe para customizar a janela de informações do marcador

    // Função para retornar a view customizada
    override fun getInfoContents(marker: Marker): View? {
        val place = marker.tag as? Place ?: return null
        val view = LayoutInflater.from(context).inflate(R.layout.custom_loc_pin_window, null)
        var textSnippet = "${place.address}\nReferência:\n${place.referencia}"
        if(place.name == "Você está aqui"){
            textSnippet = ""
        }

        val name = view.findViewById<TextView>(R.id.tvName)
        name.text = place.name
        val snippet = view.findViewById<TextView>(R.id.tvSnippet)
        snippet.text = textSnippet

        return view
    }

    override fun getInfoWindow(marker: Marker): View? {
       return null
    }
}