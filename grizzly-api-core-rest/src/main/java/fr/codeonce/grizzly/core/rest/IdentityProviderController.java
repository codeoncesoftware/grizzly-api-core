/*
 * Copyright © 2020 CodeOnce Software (https://www.codeonce.fr/)
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
package fr.codeonce.grizzly.core.rest;

import java.util.List;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.codeonce.grizzly.core.domain.identityprovider.IdentityProvider;
import fr.codeonce.grizzly.core.service.identityprovider.IdentityProviderDto;
import fr.codeonce.grizzly.core.service.identityprovider.IdentityProviderService;

@RestController
@CrossOrigin(origins = { "*" })
@RequestMapping("/api/identityprovider")
public class IdentityProviderController {

	private static final Logger log = LoggerFactory.getLogger(IdentityProviderController.class);

	@Autowired
	private IdentityProviderService identityproviderService;

	@PostMapping("/create")
	public IdentityProviderDto saveIdentityProvider(@RequestBody IdentityProviderDto dto) throws ParseException {
		log.info("request to create identityprovider with name : {}", dto.getName());
		return identityproviderService.saveIdentityProvider(dto);
	}

	@GetMapping("/all")
	public List<IdentityProviderDto> getAll() {
		log.info("request to get all identityproviders");
		return identityproviderService.getAll();
	}

	@DeleteMapping("/delete/{identityproviderId}")
	public void deleteIdentityProvider(@PathVariable String identityproviderId) {
		log.info("request to delete identity provider with ID : {}", identityproviderId);
		identityproviderService.deleteById(identityproviderId);
	}

	@DeleteMapping("/deleteByNameAndEmail/{name}/{userEmail}")
	public String deleteByName(@PathVariable String name, @PathVariable String userEmail) {
		return identityproviderService.deleteByNameAndUserEmail(name, userEmail);
	}

	@GetMapping("/check/name/{name}/{useremail}/{id}")
	public boolean checkUnicity(@PathVariable String name, @PathVariable String useremail, @PathVariable String id) {
		return identityproviderService.checkUnicity(name, useremail,id);
	}

	@GetMapping("/{identityproviderId}")
	public IdentityProviderDto getIdentityProviderById(@PathVariable String identityproviderId) {
		log.info("request to get identity provider DTO with ID : {}", identityproviderId);
		return identityproviderService.getIdentityProviderDtoById(identityproviderId);
	}

	@GetMapping("/identityProviderDisplayedName/{displayedName}")
	public Boolean existsIdentityProviderDisplayedName(@PathVariable String displayedName) {
		return identityproviderService.existsIdentityProviderDisplayedName(displayedName);
	}

	@GetMapping("/public")
	public IdentityProviderDto getIdentityProvider(@RequestParam String identityproviderId) {
		log.info("request to get IdentityProvider with ID : {}", identityproviderId);
		return identityproviderService.getIdentityProviderDtoById(identityproviderId);
	}

	@GetMapping("/public/getidentityprovider")
	public IdentityProvider getIdentityProviderEntity(@RequestParam("identityproviderId") String identityproviderId) {
		return identityproviderService.getIdentityProviderEntity(identityproviderId);
	}

	@GetMapping("/existsByName/{name}/{userEmail}")
	public Boolean existsByName(@PathVariable String name, @PathVariable String userEmail) {
		return identityproviderService.existsByNameAndUserEmail(name, userEmail);
	}

	@RequestMapping(value = "/checkConnection", method = RequestMethod.POST, consumes = {
		MediaType.APPLICATION_FORM_URLENCODED_VALUE })
	public Boolean checkodentityProviderConnection(@RequestBody MultiValueMap<String, String> body) throws ParseException {
		log.info("request to check identityprovider connection");
		return identityproviderService.checkConnection(body);
	}

	@GetMapping("/public/{identityproviderName}")
	public List<IdentityProvider> getIdentityProviderByType(@PathVariable String identityproviderName) {
		return identityproviderService.getIdentityProviderByType(identityproviderName);
	}

}
