/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.providers.x509.populator;

import junit.framework.TestCase;

import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

import org.springframework.security.providers.x509.X509TestUtils;

import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;

import org.springframework.dao.DataAccessException;

import java.security.cert.X509Certificate;


/**
 * Tests for {@link DaoX509AuthoritiesPopulator}
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class DaoX509AuthoritiesPopulatorTests extends TestCase {
    //~ Constructors ===================================================================================================

    public DaoX509AuthoritiesPopulatorTests() {
    }

    public DaoX509AuthoritiesPopulatorTests(String arg0) {
        super(arg0);
    }

    //~ Methods ========================================================================================================

    public final void setUp() throws Exception {
        super.setUp();
    }

    public void testDefaultCNPatternMatch() throws Exception {
        X509Certificate cert = X509TestUtils.buildTestCertificate();
        DaoX509AuthoritiesPopulator populator = new DaoX509AuthoritiesPopulator();

        populator.setUserDetailsService(new MockAuthenticationDaoMatchesNameOrEmail());
        populator.afterPropertiesSet();
        populator.getUserDetails(cert);
    }

    public void testEmailPatternMatch() throws Exception {
        X509Certificate cert = X509TestUtils.buildTestCertificate();
        DaoX509AuthoritiesPopulator populator = new DaoX509AuthoritiesPopulator();

        populator.setUserDetailsService(new MockAuthenticationDaoMatchesNameOrEmail());
        populator.setSubjectDNRegex("emailAddress=(.*?),");
        populator.afterPropertiesSet();
        populator.getUserDetails(cert);
    }

    public void testInvalidRegexFails() throws Exception {
        DaoX509AuthoritiesPopulator populator = new DaoX509AuthoritiesPopulator();
        populator.setUserDetailsService(new MockAuthenticationDaoMatchesNameOrEmail());
        populator.setSubjectDNRegex("CN=(.*?,"); // missing closing bracket on group

        try {
            populator.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException failed) {
            // ignored
        }
    }

    public void testMatchOnShoeSizeFieldInDNFails() throws Exception {
        X509Certificate cert = X509TestUtils.buildTestCertificate();
        DaoX509AuthoritiesPopulator populator = new DaoX509AuthoritiesPopulator();

        populator.setUserDetailsService(new MockAuthenticationDaoMatchesNameOrEmail());
        populator.setSubjectDNRegex("shoeSize=(.*?),");
        populator.afterPropertiesSet();

        try {
            populator.getUserDetails(cert);
            fail("Should have thrown BadCredentialsException.");
        } catch (BadCredentialsException failed) {
            // ignored
        }
    }

    public void testPatternWithNoGroupFails() throws Exception {
        X509Certificate cert = X509TestUtils.buildTestCertificate();
        DaoX509AuthoritiesPopulator populator = new DaoX509AuthoritiesPopulator();

        populator.setUserDetailsService(new MockAuthenticationDaoMatchesNameOrEmail());
        populator.setSubjectDNRegex("CN=.*?,");
        populator.afterPropertiesSet();

        try {
            populator.getUserDetails(cert);
            fail("Should have thrown IllegalArgumentException for regexp without group");
        } catch (IllegalArgumentException e) {
            // ignored
        }
    }

    public void testRequiresDao() throws Exception {
        DaoX509AuthoritiesPopulator populator = new DaoX509AuthoritiesPopulator();

        try {
            populator.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException failed) {
            // ignored
        }
    }

    //~ Inner Classes ==================================================================================================

    private class MockAuthenticationDaoMatchesNameOrEmail implements UserDetailsService {
        public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
            if ("Luke Taylor".equals(username) || "luke@monkeymachine".equals(username)) {
                return new User("luke", "monkey", true, true, true, true,
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_ONE")});
            } else {
                throw new UsernameNotFoundException("Could not find: " + username);
            }
        }
    }
}