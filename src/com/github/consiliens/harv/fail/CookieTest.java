package com.github.consiliens.harv.fail;

import static com.github.consiliens.harv.util.Utils.p;
import static org.junit.Assert.assertEquals;

import java.net.HttpCookie;
import java.util.List;

import org.junit.Test;

public class CookieTest {

    @Test
    public void test() {
        // HttpCookie parser doesn't parse properly.
        final String cookie = "PREF=a; NID=b";

        final List<HttpCookie> parsedCookies = HttpCookie.parse(cookie);

        for (final HttpCookie aCookie : parsedCookies)
            p(aCookie);

        // Both PREF and NID should parse.
        // Chrome dev tools successfully exports the correct cookie values.
        assertEquals("", 2, parsedCookies.size());
    }
}