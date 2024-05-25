/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import com.intellij.testFramework.fixtures.BareTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import junit.framework.TestCase;

public class BareTestFixtureTestBase extends TestCase {

    private BareTestFixture fixture;

    public void setUp() throws Exception {
        super.setUp();
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        fixture = factory.createBareFixture();
        fixture.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        fixture.tearDown();
    }

    public void testExample() {

    }
}