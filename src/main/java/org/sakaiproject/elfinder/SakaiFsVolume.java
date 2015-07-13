package org.sakaiproject.elfinder;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.content.api.*;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Volume is a container for a set of files and folder. In Sakai this will typically be the contents of a resources
 * tool or the resources for a tool in a site.
 */
public class SakaiFsVolume implements FsVolume {

    private static final Log LOG = LogFactory.getLog(SakaiFsVolume.class);

    protected SakaiFsService service;

    public String getSiteId() {
        return siteId;
    }

    // This is the ID of a site.
    // TODO What when we're not in a site?
    protected String siteId;

    public SakaiFsVolume(SakaiFsService service, String siteId) {
        this.service = service;
        this.siteId = siteId;
    }

    public void createFile(FsItem fsi) throws IOException {
        String id = service.asId(fsi);
        try {
            service.getContent().addResource(id);
        } catch (SakaiException se) {
            throw new IOException("Failed to create new file: "+ id, se);
        }

    }

    public void createFolder(FsItem fsi) throws IOException {
        String id = service.asId(fsi);
        try {
            service.getContent().addCollection(id);
        } catch (SakaiException se) {
            throw new IOException("Failed to create new folder: "+ id, se);
        }
    }

    public void deleteFile(FsItem fsi) throws IOException {
        String id = service.asId(fsi);
        try {
            service.getContent().removeResource(id);
        } catch (SakaiException se) {
            throw new IOException("Failed to remove file: "+id, se);
        }
    }

    public void deleteFolder(FsItem fsi) throws IOException {
        String id = service.asId(fsi);
        try {
            service.getContent().removeCollection(id);
        } catch (SakaiException se) {
            throw new IOException("Failed to remove folder: "+ id, se);
        }

    }

    public boolean exists(FsItem newFile) {
        try {
            String id = service.asId(newFile);
            service.getContent().getResource(id);
            return true;
        } catch (IdUnusedException iue) {
            return false; // This one we expect.
        } catch (SakaiException se) {
            return false;
        }
    }

    public FsItem fromPath(String relativePath) {
        return null;
    }

    public String getDimensions(FsItem fsi) {
        return null;
    }

    public long getLastModified(FsItem fsi) {
        String id = service.asId(fsi);
        try {
            ContentEntity contentEntity;
            if (service.getContent().isCollection(id)) {
                contentEntity = service.getContent().getCollection(id);
            } else {
                contentEntity = service.getContent().getResource(id);
            }
            Date date = contentEntity.getProperties().getDateProperty(ResourceProperties.PROP_MODIFIED_DATE);
            return date.getTime();
        } catch (SakaiException se) {
            LOG.warn("Failed to get last modified date for: "+ id, se);
        } catch (EntityPropertyTypeException e) {
            LOG.warn("Property isn't date on :"+ id, e);
        } catch (EntityPropertyNotDefinedException e) {
            // This isn't too much of a problem.
            LOG.debug("No modified date set on: "+ id, e);
        }
        return 0;
    }

    public String getMimeType(FsItem fsi) {
        String id = service.asId(fsi);
        if (service.getContent().isCollection(id)) {
            return "";
        } else {
            try {
                ContentResource resource = service.getContent().getResource(id);
                return resource.getContentType();
            } catch (SakaiException se) {
                return "";
            }
        }
    }

    public String getName() {
        return service.getSiteName(siteId);
    }

    public String getName(FsItem fsi) {
        // This is the filename.
        // This needs more test cases
        String id = service.asId(fsi);
        int lastSlash = id.lastIndexOf("/");
        if (lastSlash == id.length()) {
            lastSlash = id.lastIndexOf("/", -1);
        }
        return id.substring(lastSlash);
    }

    public FsItem getParent(FsItem fsi) {
        String id = service.asId(fsi);
        String parentId = service.getContent().getContainingCollectionId(id);
        return new SakaiFsItem(this, parentId);
    }

    public String getPath(FsItem fsi) throws IOException {
        return service.asId(fsi);
    }

    public FsItem getRoot() {
        String id = service.getContent().getSiteCollection(siteId);
        return new SakaiFsItem(this, id);
    }

    public long getSize(FsItem fsi) {
        String id = service.asId(fsi);
        try {
            if (service.getContent().isCollection(id)) {
                return service.getContent().getCollectionSize(id);
            } else {
                return service.getContent().getResource(id).getContentLength();
            }
        } catch (SakaiException se) {
            LOG.warn("Failed to get size for: "+id, se);
        }
        return 0;
    }

    public String getThumbnailFileName(FsItem fsi) {
        return null;
    }

    public boolean hasChildFolder(FsItem fsi) {
        String id = service.asId(fsi);
        try {
            ContentCollection collection = service.getContent().getCollection(id);
            // Just need to check if any of them are collections
            for (String member: collection.getMembers()) {
                if (service.getContent().isCollection(member)) {
                    // Shortcut out on the first one we find
                    return true;
                }
            }
        } catch (SakaiException se) {
            LOG.warn("Couldn't is if there are child folders: "+ id, se);
        }
        return false;
    }

    public boolean isFolder(FsItem fsi) {
        String id = service.asId(fsi);
        return service.getContent().isCollection(id);
    }

    public boolean isRoot(FsItem fsi) {
        String rootId = service.asId(getRoot());
        String id = service.asId(fsi);
        return rootId.equals(id);
    }

    public FsItem[] listChildren(FsItem fsi) {
        String id = service.asId(fsi);
        try {
            ContentCollection collection = service.getContent().getCollection(id);
            List<FsItem> items = new ArrayList<>();
            for (String member : collection.getMembers()) {
                items.add(new SakaiFsItem(this, member));
            }
            return items.toArray(new FsItem[items.size()]);
        } catch (PermissionException pe) {
            // Ignore
        } catch (SakaiException se) {
            LOG.warn("Failed to find children of: "+ id, se);
        }
        return new FsItem[0];
    }

    public InputStream openInputStream(FsItem fsi) throws IOException {
        String id = service.asId(fsi);
        try {
            ContentResource resource = service.getContent().getResource(id);
            return resource.streamContent();
        } catch (SakaiException se) {
            throw new IOException("Failed to open input stream for: "+ id, se);
        }
    }

    public OutputStream openOutputStream(final FsItem fsi) throws IOException {
        // This doesn't work at all as we are normally given an input stream which we can read our data from.
        // Stuffing it all into a byte array is horrible as we can't use heap memory
        return new ByteArrayOutputStream() {
            public void close() throws IOException {
                String id = service.asId(fsi);
                try
                {
                    ContentResourceEdit resource = service.getContent().editResource(id);
                    resource.setContent(this.toByteArray());
                    service.getContent().commitResource(resource);
                } catch ( SakaiException se )
                {
                    throw new IOException("Failed to open input stream for: " + id, se);
                }
                super.close();
            }
        };
    }

    public void rename(FsItem src, FsItem dst) throws IOException {
        String srcId = service.asId(src);
        String dstId = service.asId(dst);
        try {
            service.getContent().copy(srcId, dstId);
            if (service.getContent().isCollection(srcId)) {
                service.getContent().removeCollection(srcId);
            } else {
                service.getContent().removeResource(srcId);
            }
        } catch (SakaiException se) {
            throw new IOException("Failed to rename file: "+ srcId+ " to "+ dstId, se);
        }

    }
}
