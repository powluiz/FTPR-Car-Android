package com.example.myapitest.ui

import android.widget.ImageView
import com.example.myapitest.R
import com.squareup.picasso.Picasso

fun ImageView.loadUrl(imageUrl: String) {
    Picasso.get()
        .load(imageUrl)
        .transform(CircleTransform())
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_image_placeholder)
        .into(this)

}