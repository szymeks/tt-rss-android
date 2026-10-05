package org.fox.ttrss;

import static org.junit.Assert.assertEquals;

import org.fox.ttrss.types.Article;
import org.fox.ttrss.types.Feed;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FeedUnreadCounterTest {
    private FeedUnreadCounter counter;
    private Feed feed;

    @Before
    public void setUp() {
        counter = new FeedUnreadCounter();
        feed = feed(10, false, 5);
        counter.setFeed(feed);
    }

    private static Feed feed(int id, boolean isCat, int unread) {
        Feed feed = new Feed(id, "Feed " + id, isCat);
        feed.unread = unread;
        return feed;
    }

    private static Article article(int id, boolean unread) {
        Article article = new Article();
        article.id = id;
        article.unread = unread;
        return article;
    }

    @Test
    public void startsWithServerCounter() {
        assertEquals(5, counter.getUnread());
    }

    @Test
    public void readingArticleDecrementsAndUnreadingIncrements() {
        counter.update(feed, Arrays.asList(article(1, true), article(2, false)));
        assertEquals(5, counter.getUnread());

        counter.update(feed, Arrays.asList(article(1, false), article(2, false)));
        assertEquals(4, counter.getUnread());

        counter.update(feed, Arrays.asList(article(1, false), article(2, true)));
        assertEquals(5, counter.getUnread());
    }

    @Test
    public void articleRemovedFromListKeepsLastKnownState() {
        counter.update(feed, Collections.singletonList(article(1, true)));
        counter.update(feed, Collections.singletonList(article(1, false)));
        // e.g. "unread only" refresh drops read articles from the list
        counter.update(feed, new ArrayList<>());

        assertEquals(4, counter.getUnread());
    }

    @Test
    public void swipedArticleMarkedReadAfterRemovalFromList() {
        counter.update(feed, Arrays.asList(article(1, true), article(2, true)));
        // swipe removes the article from the list before the server confirms the read state
        counter.update(feed, Collections.singletonList(article(2, true)));
        assertEquals(5, counter.getUnread());

        counter.setUnread(1, false);
        assertEquals(4, counter.getUnread());

        // undo
        counter.setUnread(1, true);
        counter.update(feed, Arrays.asList(article(1, true), article(2, true)));
        assertEquals(5, counter.getUnread());
    }

    @Test
    public void setUnreadOnUnseenArticleAssumesStateChanged() {
        counter.setUnread(99, false);
        assertEquals(4, counter.getUnread());

        // later appearing in the list keeps the assumed initial state
        counter.update(feed, Collections.singletonList(article(99, false)));
        assertEquals(4, counter.getUnread());
    }

    @Test
    public void setUnreadTwiceCountsOnce() {
        counter.update(feed, Collections.singletonList(article(1, true)));
        counter.setUnread(1, false);
        counter.setUnread(1, false);

        assertEquals(4, counter.getUnread());
    }

    @Test
    public void setUnreadIgnoredWithoutFeed() {
        counter.setFeed(null);
        counter.setUnread(1, false);

        assertEquals(0, counter.getUnread());
    }

    @Test
    public void ignoresListLoadedForAnotherFeed() {
        counter.update(feed(11, false, 0), Arrays.asList(article(1, true), article(2, true),
                article(3, true), article(4, true), article(5, true), article(6, true)));
        counter.update(feed(11, false, 0), Collections.singletonList(article(1, false)));

        assertEquals(5, counter.getUnread());
    }

    @Test
    public void categoryWithSameIdIsDifferentFeed() {
        counter.update(feed(10, true, 0), Collections.singletonList(article(1, true)));
        counter.update(feed(10, true, 0), Collections.singletonList(article(1, false)));

        assertEquals(5, counter.getUnread());
    }

    @Test
    public void neverBelowUnreadArticlesInList() {
        Feed shortcut = feed(20, false, 0);
        counter.setFeed(shortcut);

        counter.update(shortcut, Arrays.asList(article(1, true), article(2, true), article(3, false)));
        assertEquals(2, counter.getUnread());
    }

    @Test
    public void neverNegative() {
        Feed stale = feed(20, false, 1);
        counter.setFeed(stale);
        counter.update(stale, Arrays.asList(article(1, true), article(2, true)));
        counter.update(stale, Arrays.asList(article(1, false), article(2, false)));

        assertEquals(0, counter.getUnread());
    }

    @Test
    public void sameFeedWithSameCounterKeepsLocalChanges() {
        counter.update(feed, Collections.singletonList(article(1, true)));
        counter.update(feed, Collections.singletonList(article(1, false)));

        // e.g. DetailActivity receives a parcelled copy of the active feed
        counter.setFeed(feed(10, false, 5));

        assertEquals(4, counter.getUnread());
    }

    @Test
    public void newServerCounterResetsLocalChanges() {
        counter.update(feed, Collections.singletonList(article(1, true)));
        counter.update(feed, Collections.singletonList(article(1, false)));

        counter.setFeed(feed(10, false, 4));
        counter.update(feed, Collections.singletonList(article(1, false)));

        assertEquals(4, counter.getUnread());
    }

    @Test
    public void switchingFeedResets() {
        counter.update(feed, Collections.singletonList(article(1, true)));
        counter.update(feed, Collections.singletonList(article(1, false)));

        Feed other = feed(11, false, 7);
        counter.setFeed(other);

        assertEquals(7, counter.getUnread());
    }

    @Test
    public void clearMarksTrackedFeedRead() {
        counter.update(feed, Collections.singletonList(article(1, true)));
        counter.clear(feed(11, false, 0));
        assertEquals(5, counter.getUnread());

        counter.clear(feed);
        counter.update(feed, Collections.singletonList(article(1, false)));
        assertEquals(0, counter.getUnread());
    }

    @Test
    public void nullFeedStopsTracking() {
        counter.setFeed(null);
        counter.update(feed, Collections.singletonList(article(1, true)));

        assertEquals(0, counter.getUnread());
    }

    @Test
    public void formatTitlePrefixesUnreadCount() {
        assertEquals("(3) News", FeedUnreadCounter.formatTitle("News", 3));
        assertEquals("News", FeedUnreadCounter.formatTitle("News", 0));
        assertEquals("(1) ", FeedUnreadCounter.formatTitle(null, 1));
        assertEquals(null, FeedUnreadCounter.formatTitle(null, 0));
    }
}
