package org.sakaiproject.elfinder.sakai;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.content.api.*;
import org.sakaiproject.elfinder.impl.SakaiFsService;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
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
        	String filename = getLocalName(fsi);
        	String name="", ext="";
        	int index = filename.lastIndexOf(".");
        	if(index >= 0){
        		name = filename.substring(0, index);
        		ext = filename.substring(index+1);
        	}
        	ContentResourceEdit cre = service.getContent().addResource(service.asId(getParent(fsi)), name, ext, 999); 
            service.getContent().commitResource(cre);
            //update saved id
            ((SakaiFsItem)fsi).setId(cre.getId());
            
        } catch (SakaiException se) {
            throw new IOException("Failed to create new file: "+ id, se);
        }

    }

    public void createFolder(FsItem fsi) throws IOException {
    	String id = service.asId(fsi);
    	try
    	{
    		String collectionId = service.asId(getParent(fsi));
    		String name = getLocalName(fsi);
    		ContentCollectionEdit edit = service.getContent().addCollection(collectionId, name);
    		ResourcePropertiesEdit props = edit.getPropertiesEdit();
    		props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);

    		service.getContent().commitCollection(edit);
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
            if (service.getContent().isCollection(id)) {
                service.getContent().getCollection(id);
            } else {
                service.getContent().getResource(id);
            }
            return true;
        } catch (IdUnusedException iue) {
            return false; // This one we expect.
        } catch (SakaiException se) {
            return false;
        }
    }

    public FsItem fromPath(String path) {
        return new SakaiFsItem(this, path);
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
            return "directory";
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
        String id = service.asId(fsi);
        try {
        	//ask ContentHostingService for name
            ContentEntity contentEntity;
            if (service.getContent().isCollection(id)) {
                contentEntity = service.getContent().getCollection(id);
            } else {
                contentEntity = service.getContent().getResource(id);
            }
            return contentEntity.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME);
        } catch (SakaiException se) {
            LOG.warn("Failed to get name for: "+ id, se);
        }
        return id;
    }
    
    public String getLocalName(FsItem fsi) {
    	return service.getLocalName(fsi);
    }

    public FsItem getParent(FsItem fsi) {
        String id = service.asId(fsi);
        String parentId = service.getContent().getContainingCollectionId(id);
        return new SakaiFsItem(this, parentId);
    }

    public String getPath(FsItem fsi) throws IOException {
    	String id = service.asId(fsi);    	
    	int lastSlash = id.lastIndexOf("/");
         
        if(lastSlash < 0) return id;
        
        return id.substring(0, lastSlash);
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
    	//not needed
        return null;
    }

    public void rename(FsItem src, FsItem dst) throws IOException {
    	String srcId = service.asId(src);
        String dstName = getLocalName(dst);
        
        try {
        	if (service.getContent().isCollection(srcId)) {
        		ContentCollectionEdit edit = service.getContent().editCollection(srcId);
        		ResourcePropertiesEdit props = edit.getPropertiesEdit();
        		props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, dstName);

        		service.getContent().commitCollection(edit);
            } else {
            	ContentResourceEdit edit = service.getContent().editResource(srcId);
            	ResourcePropertiesEdit props = edit.getPropertiesEdit();
        		props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, dstName);

        		service.getContent().commitResource(edit);
            }
        } catch(SakaiException se) {
        	throw new IOException("Failed to rename file: "+ srcId+ " to "+ dstName, se);
        }
    }
}
