package org.sakaiproject.elfinder.sakai.content;

import cn.bluejoe.elfinder.controller.ErrorException;
import cn.bluejoe.elfinder.service.FsItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.*;
import org.sakaiproject.elfinder.sakai.SakaiFsService;
import org.sakaiproject.elfinder.sakai.SiteVolume;
import org.sakaiproject.elfinder.sakai.site.SiteFsItem;
import org.sakaiproject.elfinder.sakai.site.SiteFsVolume;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.SakaiException;
import org.sakaiproject.site.api.SiteService;

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
public class ContentFsVolume implements SiteVolume {

    private static final Log LOG = LogFactory.getLog(ContentFsVolume.class);

    protected ContentHostingService content;

    protected SiteService siteService;

    protected SakaiFsService service;

    public String getSiteId() {
        return siteId;
    }

    @Override
    public String getPrefix() {
        return "content";
    }

    // This is the ID of a site.
    // TODO What when we're not in a site?
    protected String siteId;

    public ContentFsVolume(String siteId, SakaiFsService service) {
        this.siteId = siteId;
        this.service = service;
        // Todo inject
        content = (ContentHostingService) ComponentManager.get(ContentHostingService.class);
        siteService = (SiteService) ComponentManager.get(SiteService.class);
    }

    public void createFile(FsItem fsi) throws IOException {
        String id = asId(fsi);
        try {
            String filename = lastPathSegment(id);
            String name = "", ext = "";
            int index = filename.lastIndexOf(".");
            if (index >= 0) {
                name = filename.substring(0, index);
                ext = filename.substring(index + 1);
            }
            ContentResourceEdit cre = content.addResource(asId(getParent(fsi)), name, ext, 999);
            content.commitResource(cre);
            //update saved ID incase it wasn't the same
            ((ContentFsItem) fsi).setId(cre.getId());

        } catch (SakaiException se) {
            throw new IOException("Failed to create new file: " + id, se);
        }

    }

    public void createFolder(FsItem fsi) throws IOException {
        String id = asId(fsi);
        try {
            String collectionId = asId(getParent(fsi));
            String path = asId(fsi);
            String name = lastPathSegment(path);
            ContentCollectionEdit edit = content.addCollection(collectionId, name);
            ResourcePropertiesEdit props = edit.getPropertiesEdit();
            props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);

            content.commitCollection(edit);
        } catch (SakaiException se) {
            throw new IOException("Failed to create new folder: " + id, se);
        }
    }

    /**
     * This extracts the last name from the path.
     *
     * @param path The path, can end with a '/' or the filename.
     * @return The name of the last name in the path with no slashes.
     */
    private String lastPathSegment(String path) {
        int start = path.lastIndexOf("/");
        int stop = path.length();
        if (start == path.length() - 1) {
            stop = start;
            start = path.lastIndexOf("/", start - 1);
        }
        return path.substring(start + 1, stop);
    }

    public void deleteFile(FsItem fsi) throws IOException {
        String id = asId(fsi);
        try {
            content.removeResource(id);
        } catch (SakaiException se) {
            throw new IOException("Failed to remove file: " + id, se);
        }
    }

    public void deleteFolder(FsItem fsi) throws IOException {
        String id = asId(fsi);
        try {
            content.removeCollection(id);
        } catch (SakaiException se) {
            throw new IOException("Failed to remove folder: " + id, se);
        }

    }

    public boolean exists(FsItem newFile) {
        try {
            String id = asId(newFile);
            if (content.isCollection(id)) {
                content.getCollection(id);
            } else {
                content.getResource(id);
            }
            return true;
        } catch (IdUnusedException iue) {
            return false; // This one we expect.
        } catch (SakaiException se) {
            return false;
        }
    }

    public FsItem fromPath(String path) {
        // The path is relative to the site's top level folder.
        if (path == null) {
            return getRoot();
        } else {
            return new ContentFsItem(this, path);
        }
    }

    public String getDimensions(FsItem fsi) {
        return null;
    }

    public long getLastModified(FsItem fsi) {
        String id = asId(fsi);
        try {
            ContentEntity contentEntity;
            if (content.isCollection(id)) {
                contentEntity = content.getCollection(id);
            } else {
                contentEntity = content.getResource(id);
            }
            Date date = contentEntity.getProperties().getDateProperty(ResourceProperties.PROP_MODIFIED_DATE);
            return date.getTime();
        } catch (SakaiException se) {
            LOG.warn("Failed to get last modified date for: " + id, se);
        } catch (EntityPropertyTypeException e) {
            LOG.warn("Property isn't date on :" + id, e);
        } catch (EntityPropertyNotDefinedException e) {
            // This isn't too much of a problem.
            LOG.debug("No modified date set on: " + id, e);
        }
        return 0;
    }

    public String getMimeType(FsItem fsi) {
        String id = asId(fsi);
        if (content.isCollection(id)) {
            return "directory";
        } else {
            try {
                ContentResource resource = content.getResource(id);
                return resource.getContentType();
            } catch (SakaiException se) {
                return "";
            }
        }
    }

    public String getName() {
        try {
            return siteService.getSite(siteId).getTitle() + " Resources";
        } catch (SakaiException se) {
            return "unknown";
        }
    }

    public String getName(FsItem fsi) {
        String rootId = asId(getRoot());
        String id = asId(fsi);
        if (rootId.equals(id)) {
            // Todo this needs i18n
            return "Resources";
        }
        try {
            //ask ContentHostingService for name
            ContentEntity contentEntity;
            if (content.isCollection(id)) {
                contentEntity = content.getCollection(id);
            } else {
                contentEntity = content.getResource(id);
            }
            return contentEntity.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME);
        } catch (SakaiException se) {
            LOG.warn("Failed to get name for: " + id, se);
        }
        return id;
    }

    public FsItem getParent(FsItem fsi) {
        String rootId = asId(getRoot());
        String id1 = asId(fsi);
        if (!rootId.equals(id1)) {
            String id = asId(fsi);
            String parentId = content.getContainingCollectionId(id);
            return fromPath(parentId);
        } else {
            // Really this shouldn't be here
            return new SiteFsItem(new SiteFsVolume(siteId, service), siteId);
        }
    }

    public String getPath(FsItem fsi) throws IOException {
        String id = asId(fsi);
        // This is need because FsItemEx enforces the slash between directory and file
        // and Sakai directories always have trailing /
        return id;
//    	int lastSlash = id.lastIndexOf("/");
//        if(lastSlash < 0) return id;
//        return id.substring(0, lastSlash);
    }

    public FsItem getRoot() {
        String id = content.getSiteCollection(siteId);
        return fromPath(id);
    }

    public long getSize(FsItem fsi) {
        String id = asId(fsi);
        try {
            if (content.isCollection(id)) {
                return content.getCollectionSize(id);
            } else {
                return content.getResource(id).getContentLength();
            }
        } catch (SakaiException se) {
            LOG.warn("Failed to get size for: " + id, se);
        }
        return 0;
    }

    public String getThumbnailFileName(FsItem fsi) {
        return null;
    }

    public boolean hasChildFolder(FsItem fsi) {
        String id = asId(fsi);
        try {
            // For sites that don't have a root folder yet this will fail.
            ContentCollection collection = content.getCollection(id);
            // Just need to check if any of them are collections
            for (String member : collection.getMembers()) {
                if (content.isCollection(member)) {
                    // Shortcut out on the first one we find
                    return true;
                }
            }
        } catch (SakaiException se) {
            LOG.warn("Couldn't is if there are child folders: " + id, se);
        }
        return false;
    }

    public boolean isFolder(FsItem fsi) {
        String id = asId(fsi);
        return content.isCollection(id);
    }

    /**
     * For a SubVolume this must always be false so it walks back up the hierarchy.
     */
    public boolean isRoot(FsItem fsi) {
        return false;
    }

    public FsItem[] listChildren(FsItem fsi) {
        String id = asId(fsi);
        try {
            ContentCollection collection = content.getCollection(id);
            List<FsItem> items = new ArrayList<>();
            for (String member : collection.getMembers()) {
                items.add(fromPath(member));
            }
            return items.toArray(new FsItem[items.size()]);
        } catch (PermissionException pe) {
            throw new ErrorException("errPerm");
        } catch (SakaiException se) {
            LOG.warn("Failed to find children of: " + id, se);
        }
        return new FsItem[0];
    }

    public InputStream openInputStream(FsItem fsi) throws IOException {
        String id = asId(fsi);
        try {
            ContentResource resource = content.getResource(id);
            return resource.streamContent();
        } catch (SakaiException se) {
            throw new IOException("Failed to open input stream for: " + id, se);
        }
    }

    public OutputStream openOutputStream(final FsItem fsi) throws IOException {
        //not needed
        return null;
    }

    public void rename(FsItem src, FsItem dst) throws IOException {
        String srcId = asId(src);
        String dstName = getName(dst);

        try {
            if (content.isCollection(srcId)) {
                ContentCollectionEdit edit = content.editCollection(srcId);
                ResourcePropertiesEdit props = edit.getPropertiesEdit();
                props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, dstName);

                content.commitCollection(edit);
            } else {
                ContentResourceEdit edit = content.editResource(srcId);
                ResourcePropertiesEdit props = edit.getPropertiesEdit();
                props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, dstName);

                content.commitResource(edit);
            }
        } catch (SakaiException se) {
            throw new IOException("Failed to rename file: " + srcId + " to " + dstName, se);
        }
    }

    public String asId(FsItem fsItem) {
        if (fsItem instanceof ContentFsItem) {
            return ((ContentFsItem) fsItem).getId();
        } else {
            throw new IllegalArgumentException("Passed FsItem must be a SakaiFsItem.");
        }
    }
}

