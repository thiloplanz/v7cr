/**
 * Copyright (c) 2011, Thilo Planz. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package v7cr;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.expressme.openid.Association;
import org.expressme.openid.Authentication;
import org.expressme.openid.Endpoint;
import org.expressme.openid.OpenIdManager;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@SuppressWarnings("serial")
public class OpenIDServlet extends HttpServlet {

	static final String OPENID_AUTHENTICATION = "jopenid.authentication";

	private static final String ATTR_MAC = "jopenid.mac";

	private static final String ATTR_ALIAS = "jopenid.alias";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		HttpSession session = request.getSession();
		if (session.getAttribute(OPENID_AUTHENTICATION) == null) {
			OpenIdManager manager = new OpenIdManager();
			String realm = request.getScheme() + "://"
					+ request.getServerName() + ":" + request.getServerPort();
			manager.setRealm(realm);
			manager.setReturnTo(realm + request.getContextPath()
					+ request.getServletPath());
			System.out.println(realm);
			System.out.println(request.getContextPath()
					+ request.getServletPath());
			String op = request.getParameter("op");
			if (op == null) {
				String nonce = request.getParameter("openid.response_nonce");
				// check nonce:
				checkNonce(nonce);
				// get authentication:
				byte[] mac_key = (byte[]) session.getAttribute(ATTR_MAC);
				if (mac_key == null) {
					throw new SecurityException(
							"session expired, please try again");
				}
				String alias = (String) session.getAttribute(ATTR_ALIAS);
				Authentication authentication = manager.getAuthentication(
						request, mac_key, alias);
				String email = authentication.getEmail();

				if (email == null || email.isEmpty())
					throw new SecurityException("email address is required");

				session.setAttribute(OPENID_AUTHENTICATION, authentication);

				response.sendRedirect(request.getContextPath() + "/v/");
				return;
			}
			// redirect to sign on page:
			Endpoint endpoint = manager.lookupEndpoint(op);
			Association association = manager.lookupAssociation(endpoint);
			session.setAttribute(ATTR_MAC, association.getRawMacKey());
			session.setAttribute(ATTR_ALIAS, endpoint.getAlias());
			String url = manager.getAuthenticationUrl(endpoint, association);
			response.sendRedirect(url);
			return;

		}

	}

	private void checkNonce(String nonce) {
		if (nonce == null)
			throw new SecurityException("openid.nonce is missing");

		DBCollection nonces = InitDB.getDB(getServletContext()).getCollection(
				"openid.nonce");
		DBObject c = nonces.findOne(nonce);
		if (c != null)
			throw new SecurityException("openid.nonce " + nonce
					+ " has been used before");
		nonces.save(new BasicDBObject("_id", nonce));
	}

}
