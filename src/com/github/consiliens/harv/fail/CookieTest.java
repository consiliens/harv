package com.github.consiliens.harv.fail;

import static com.github.consiliens.harv.util.Utils.p;

import java.net.HttpCookie;
import java.util.List;

public class CookieTest {

    // HttpCookie parser doesn't parse properly.
    public static void main(String[] args) {
        String cookie = "PREF=a; NID=b";

        List<HttpCookie> parsedCookies = HttpCookie.parse(cookie);
        
        // Both PREF and NID should parse.
        // Chrome dev tools successfully exports the correct cookie values.
        p(parsedCookies.size() == 2);
        
        for (HttpCookie aCookie : parsedCookies) {
            p(aCookie);
        }
    }
}
