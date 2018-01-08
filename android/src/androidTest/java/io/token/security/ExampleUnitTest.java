package io.token.security;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import junit.framework.TestCase;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ExampleUnitTest{
//    @Rule
//    public ActivityTestRule<Activity> mActivityRule =
//            new ActivityTestRule<>(Activity.class);

    @Test
    public void testAdd(){
//        AKSCryptoEngineFactory cef =
//                new AKSCryptoEngineFactory(mActivityRule.getActivity().getApplicationContext());
//        CryptoEngine aksce = cef.create("abc");

        assertEquals(4, 2 + 2);
    }
}