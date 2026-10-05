package org.fox.ttrss;

import org.fox.ttrss.types.Article;
import org.fox.ttrss.types.Feed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the unread count of the active feed shown in the toolbar title.
 * Starts from the server counter captured when the feed was opened and
 * adjusts it by read state changes of articles seen in the headlines list.
 */
final class FeedUnreadCounter {
    private Feed m_feed;
    private int m_base;
    private int m_listUnread;
    private final Map<Integer, Boolean> m_initialUnread = new HashMap<>();
    private final Map<Integer, Boolean> m_lastUnread = new HashMap<>();

    /** Starts tracking a feed; keeps local changes if it is the same feed with an unchanged counter. */
    void setFeed(Feed feed) {
        if (feed == null) {
            m_feed = null;
            reset(0);
            return;
        }

        if (feed.equals(m_feed) && feed.unread == m_base)
            return;

        m_feed = new Feed(feed.id, feed.title, feed.is_cat);
        reset(feed.unread);
    }

    /** All articles of the tracked feed were marked as read on the server. */
    void clear(Feed feed) {
        if (feed != null && feed.equals(m_feed))
            reset(0);
    }

    /** Records read state of articles; ignored unless the list was loaded for the tracked feed. */
    void update(Feed articlesFeed, List<Article> articles) {
        if (m_feed == null || articles == null || !m_feed.equals(articlesFeed))
            return;

        m_listUnread = 0;

        for (Article article : articles) {
            m_initialUnread.putIfAbsent(article.id, article.unread);
            m_lastUnread.put(article.id, article.unread);

            if (article.unread)
                m_listUnread++;
        }
    }

    int getUnread() {
        int count = m_base;

        for (Map.Entry<Integer, Boolean> entry : m_initialUnread.entrySet()) {
            boolean initial = entry.getValue();
            boolean last = Boolean.TRUE.equals(m_lastUnread.get(entry.getKey()));

            if (initial && !last)
                count--;
            else if (!initial && last)
                count++;
        }

        return Math.max(Math.max(count, 0), m_listUnread);
    }

    static String formatTitle(String title, int unread) {
        if (unread <= 0)
            return title;

        return "(" + unread + ") " + (title != null ? title : "");
    }

    private void reset(int base) {
        m_base = base;
        m_listUnread = 0;
        m_initialUnread.clear();
        m_lastUnread.clear();
    }
}
