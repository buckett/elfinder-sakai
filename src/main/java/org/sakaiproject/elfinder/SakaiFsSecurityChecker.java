package org.sakaiproject.elfinder;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsService;

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
        String id =  service.asId(fsi);
        // TODO Should this be an UUID or a ID
        return service.getContent().isLocked(id);
    }

    @Override
    public boolean isReadable(FsService fsService, FsItem fsi) throws IOException {
        // All filtering is done by the CHS
        return true;
    }

    @Override
    public boolean isWritable(FsService fsService, FsItem fsi) throws IOException {
        String id =  service.asId(fsi);
        return service.getContent().allowAddResource(id);
    }
}
