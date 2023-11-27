package com.dynatech2012.kamleonuserapp.base

import androidx.core.content.FileProvider
import com.dynatech2012.kamleonuserapp.R

class MyFileProvider: FileProvider(R.xml.file_paths) {
    /*public MyFileProvider() {
        super(R.xml.file_paths)
    }*/
}