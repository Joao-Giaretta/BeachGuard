package com.beachguard.projeto3_equipe26.components

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng

data class Place(
    val id: String,
    val name: String,
    val latLng: LatLng,
    val address: String,
    val referencia: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        LatLng(parcel.readDouble(), parcel.readDouble()),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeDouble(latLng.latitude)
        parcel.writeDouble(latLng.longitude)
        parcel.writeString(address)
        parcel.writeString(referencia)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Place> {
        override fun createFromParcel(parcel: Parcel): Place {
            return Place(parcel)
        }

        override fun newArray(size: Int): Array<Place?> {
            return arrayOfNulls(size)
        }
    }
}
