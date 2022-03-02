package it.Ettore.egalfilemanager;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import org.junit.Test;

import androidx.test.InstrumentationRegistry;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class StorageUtilsTest {
    private StoragesUtils storagesUtils;


    public StorageUtilsTest(){
        storagesUtils = new StoragesUtils(InstrumentationRegistry.getTargetContext());
    }


    @Test
    public void testGetInternalStorage(){
        assertNotNull(storagesUtils.getInternalStorage());
    }


    @Test
    public void testGetExternalStorages(){
        assertNotNull(storagesUtils.getExternalStorages());
    }


    @Test
    public void testGetAllStorages(){
        assertTrue(storagesUtils.getAllStorages().size() > 0);
    }


    @Test
    public void testGetExtStoragePathForFileNull(){
        assertNull(storagesUtils.getExtStoragePathForFile(null));
    }


}
