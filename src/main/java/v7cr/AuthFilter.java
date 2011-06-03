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
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.expressme.openid.Authentication;

import v7cr.v7db.AccountInfo;
import v7cr.v7db.Role;
import v7cr.v7db.Roles;
import v7cr.v7db.SessionInfo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class AuthFilter implements Filter {

	private ServletContext context;

	public void destroy() {
	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {

		if (req instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpSession session = request.getSession(false);

			if (session == null || session.getAttribute("v7cr.user") == null) {

				Authentication auth = null;
				if (session != null) {
					auth = (Authentication) session
							.getAttribute(OpenIDServlet.OPENID_AUTHENTICATION);
				}

				if (auth == null) {
					if (session == null)
						session = request.getSession();
					auth = new Authentication();
					auth.setFullname("Test User");
					auth.setEmail("test@test.com");
				}

				if (auth == null) {

					// check if an admin has already been registered
					Role admins = Roles.load(InitDB.getDBCollection(context,
							"roles"), "admin");
					if (admins.getMembers().isEmpty()) {
						((HttpServletResponse) resp).sendRedirect(request
								.getContextPath()
								+ "/install.html");
						return;
					}

					((HttpServletResponse) resp).sendRedirect(request
							.getContextPath()
							+ "/login.html");
					return;
				}
				AccountInfo account = new AccountInfo(auth.getEmail(), auth
						.getFullname());

				// check if this ID can log in
				Map<String, Role> roles = Roles.loadRoles(InitDB
						.getDBCollection(context, "roles"), account.getId());

				if (!roles.containsKey("connect")) {
					// check if an admin has already been registered
					DBCollection ac = InitDB.getDBCollection(context, "roles");
					Role admins = Roles.load(ac, "admin");
					if (admins.getMembers().isEmpty()) {
						admins = admins.addMember(account);
						ac.save(new BasicDBObject(admins.getBSONObject()));
						roles = Roles.loadRoles(InitDB.getDBCollection(context,
								"roles"), account.getId());
					}
				}

				if (!roles.containsKey("connect")) {
					throw new SecurityException(account.getId()
							+ " is not allowed to connect");
				}
				SessionInfo sessionInfo = new SessionInfo();
				sessionInfo.accountInfo = account;
				sessionInfo.roles = roles;
				session.setAttribute("v7cr.sessionInfo", sessionInfo);
				session.setAttribute("v7cr.user", account);
			}
		}
		chain.doFilter(req, resp);

	}

	public void init(FilterConfig conf) throws ServletException {
		context = conf.getServletContext();
	}

}
