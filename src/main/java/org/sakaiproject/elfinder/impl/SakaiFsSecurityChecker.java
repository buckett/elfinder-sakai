package org.sakaiproject.elfinder.impl;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsService;
import org.sakaiproject.elfinder.sakai.ReadOnlyFsVolume;
import org.sakaiproject.elfinder.sakai.SakaiFsService;

import java.io.IOException;

/**
 * Created by buckett on 10/07/15.
 */
public class SakaiFsSecurityChecker implements FsSecurityChecker {

    private SakaiFsService service;

    public SakaiFsSecurityChecker(SakaiFsService service) {
        this.service = service;
    }


    @Override
    public boolean isLocked(FsService fsService, FsItem fsi) throws IOException {
//        String id =  service.asId(fsi);
        // TODO Should this be an UUID or a ID
        //
        return false;
//        return service.getContent().isLocked(id);
    }

    @Override
    public boolean isReadable(FsService fsService, FsItem fsi) throws IOException {
        // All filtering should be done by the volumes.
        return true;
    }

    @Override
    public boolean isWritable(FsService fsService, FsItem fsi) throws IOException {
        if (fsService instanceof ReadOnlyFsVolume) {
            return false;
        } else {
            // TODO this needs to work across all volumes
           return true;
        }
    }
}
