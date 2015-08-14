package org.sakaiproject.elfinder.sakai.msgcntr;

import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.service.FsItem;
import org.sakaiproject.api.app.messageforums.*;
import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.elfinder.sakai.ReadOnlyFsVolume;
import org.sakaiproject.elfinder.sakai.SakaiFsService;
import org.sakaiproject.elfinder.sakai.SiteVolume;
import org.sakaiproject.elfinder.sakai.site.SiteFsItem;
import org.sakaiproject.elfinder.sakai.site.SiteFsVolume;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Area - Toplevel (lookup by siteId)
 * Forums - In Area,
 * Topics - In forums.
 */
public class MsgCntrFsVolume extends ReadOnlyFsVolume implements SiteVolume {

    private SakaiFsService service;
    private String siteId;
    private AreaManager areaManager;
    private MessageForumsForumManager forumManager;
    private DiscussionForumManager discussionForumManager;

    public MsgCntrFsVolume(SakaiFsService service, String siteId) {
        this.service = service;
        this.siteId = siteId;
        areaManager = (AreaManager) ComponentManager.get(AreaManager.class);
        discussionForumManager = (DiscussionForumManager) ComponentManager.get(DiscussionForumManager.class);
        forumManager = (MessageForumsForumManager) ComponentManager.get(MessageForumsForumManager.class);
    }

    @Override
    public String getSiteId() {
        return siteId;
    }

    @Override
    public String getPrefix() {
        return "msgcntr";
    }

    @Override
    public boolean exists(FsItem newFile) {
        return false;
    }

    @Override
    public FsItem fromPath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return getRoot();
        }
        String[] parts = relativePath.split("/");
        if (parts.length > 2) {
            if ("forum".equals(parts[1])) {
                String forumId = parts[2];
                BaseForum forum = forumManager.getForumByUuid(forumId);
                return new ForumMsgCntrFsItem(forum, "", this);
            } else if ("topic".equals(parts[1])) {
                String topicId = parts[2];
                Topic topic = forumManager.getTopicByUuid(topicId);
                return new TopicMsgCntrFsItem(topic, "", this);
            }
        }
        return getRoot();
    }

    @Override
    public String getPath(FsItem fsi) throws IOException {
        if (getRoot().equals(fsi)) {
            return "";
        } else if (fsi instanceof ForumMsgCntrFsItem) {
            ForumMsgCntrFsItem forumFsi = (ForumMsgCntrFsItem)fsi;
            return "/forum/"+ forumFsi.getForum().getUuid();
        } else if (fsi instanceof TopicMsgCntrFsItem) {
            TopicMsgCntrFsItem topicMsgCntrFsItem = (TopicMsgCntrFsItem)fsi;
            return "/topic/"+topicMsgCntrFsItem.getTopic().getUuid();
        }
        throw new IllegalArgumentException("Wrong type: "+fsi);
    }

    @Override
    public String getDimensions(FsItem fsi) {
        return null;
    }

    @Override
    public long getLastModified(FsItem fsi) {
        return 0;
    }

    @Override
    public String getMimeType(FsItem fsi) {
        return isFolder(fsi)?"directory":"sakai/forums";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getName(FsItem fsi) {
        // TODO go into item
        if (getRoot().equals(fsi)) {
            // Todo this needs i18n
            return "Forums";
        } else if (fsi instanceof ForumMsgCntrFsItem){
            return ((ForumMsgCntrFsItem)fsi).getForum().getTitle();
        } else if (fsi instanceof TopicMsgCntrFsItem) {
            return ((TopicMsgCntrFsItem)fsi).getTopic().getTitle();
        } else {
            throw new IllegalArgumentException("Could not get title for: "+ fsi.toString());
        }
    }

    @Override
    public FsItem getParent(FsItem fsi) {
        if (getRoot().equals(fsi)) {
            return new SiteFsItem(new SiteFsVolume(siteId, service), siteId);
        }
        // TODO Move to item
        if (fsi instanceof ForumMsgCntrFsItem) {
            return getRoot();
        }
        if (fsi instanceof TopicMsgCntrFsItem) {
            Topic topic = ((TopicMsgCntrFsItem) fsi).getTopic();
            // This is just horrible
            Topic topicAndParent = forumManager.getTopicById(true, topic.getId());
            return new ForumMsgCntrFsItem(topicAndParent.getBaseForum(), "", this);
        }
        return null;
    }


    @Override
    public FsItem getRoot() {
        return new MsgCntrFsItem("", this);
    }

    @Override
    public long getSize(FsItem fsi) throws IOException {
        // TODO
        return 0;
    }

    @Override
    public String getThumbnailFileName(FsItem fsi) {
        return null;
    }

    @Override
    public boolean hasChildFolder(FsItem fsi) {
        return true;
    }

    @Override
    public boolean isFolder(FsItem fsi) {
        return ! (fsi instanceof TopicMsgCntrFsItem);
    }

    @Override
    public boolean isRoot(FsItem fsi) {
        // Always false as we have SiteFsVolumes below.
        return false;
    }

    @Override
    public FsItem[] listChildren(FsItem fsi) {
        // Should be in the items
        List<FsItem> items = new ArrayList<>();
        if (getRoot().equals(fsi)) {
            List<DiscussionForum> discussionForums = discussionForumManager.getDiscussionForumsByContextId(siteId);
            for (DiscussionForum discussionForum : discussionForums) {
                discussionForum.getTitle();
                ForumMsgCntrFsItem childFsi = new ForumMsgCntrFsItem(discussionForum, "", this);
                items.add(childFsi);
            }
        } else if (fsi instanceof ForumMsgCntrFsItem) {
            BaseForum forum = ((ForumMsgCntrFsItem)fsi).getForum();
            BaseForum forumAndTopics = forumManager.getForumByIdWithTopics(forum.getId());
            for (Topic topic: (List<Topic>) forumAndTopics.getTopics()) {
                TopicMsgCntrFsItem childFsi = new TopicMsgCntrFsItem(topic, "", this);
                items.add(childFsi);
            }

        }

        return items.toArray(new FsItem[0]);

    }

    @Override
    public InputStream openInputStream(FsItem fsi) throws IOException {
        return null;
    }

    @Override
    public String getURL(FsItem f) {
        return null;
    }

}
