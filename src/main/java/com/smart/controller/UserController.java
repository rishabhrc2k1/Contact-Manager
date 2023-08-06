package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	@ModelAttribute
	public void addCommonData(Model m, Principal principal) {
		String userName = principal.getName();

		User user = userRepository.getUserByUserName(userName);

		m.addAttribute("user", user);

	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {

		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";

	}

	@RequestMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {
			String name = principal.getName();

			User user = this.userRepository.getUserByUserName(name);

			if (file.isEmpty()) {

				contact.setImage("contact.png");

			} else {
				contact.setImage(file.getOriginalFilename());

				File savedFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(savedFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);

			session.setAttribute("message", new Message("Your contact is added!!", "success"));

		} catch (Exception e) {
			System.out.println(e);
			session.setAttribute("message", new Message("Something went wrong", "danger"));
		}
		return "normal/add_contact_form";
	}

	@GetMapping("/show_contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		PageRequest pageable = PageRequest.of(page, 2);

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);

		m.addAttribute("totalPages", contacts.getTotalPages());

		m.addAttribute("title", "Show Contacts");
		return "normal/show_contacts";
	}

	@RequestMapping("/contact/{cid}")
	public String showContactsDetail(@PathVariable("cid") Integer cId, Model model, Principal principal) {
		System.out.println(cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("title", contact.getName());
			model.addAttribute("contact", contact);
		}

		return "normal/contact_detail";
	}

	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Principal principal, HttpSession session) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		contact.setUser(null);

		this.contactRepository.delete(contact);
		session.setAttribute("message", new Message("Contact deleted successfully.....", "success"));

		return "redirect:/user/show_contacts/0";

	}

	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {

		m.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cid).get();

		m.addAttribute("contact", contact);

		return "normal/update_form";
	}

	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,
			 Model m,Principal principal)
	{
		try {
			Contact oldcontactDetails=this.contactRepository.findById(contact.getCid()).get();
			
			if(!file.isEmpty())
			{
				
				
				
				
				File savedFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(savedFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
			}else {
				contact.setImage(oldcontactDetails.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			System.out.println(contact.getName());
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return"";
	}

}
