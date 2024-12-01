package com.ninjaone.dundie_awards;

import static com.ninjaone.dundie_awards.model.Organization.FRAJOLA;
import static com.ninjaone.dundie_awards.model.Organization.GARFIELD;
import static com.ninjaone.dundie_awards.model.Organization.HELLO_KITTY;
import static com.ninjaone.dundie_awards.model.Organization.SQUANCHY;
import static com.ninjaone.dundie_awards.model.Organization.TOM;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import com.ninjaone.dundie_awards.repository.EmployeeRepository;
import com.ninjaone.dundie_awards.repository.OrganizationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final AwardsCache awardsCache;
    private final AppProperties appProperties;

    @Override
    public void run(String... args) {
        // uncomment to reseed data
        // employeeRepository.deleteAll();
        // organizationRepository.deleteAll();

        if (employeeRepository.count() == 0) {
        	
        	List<Organization> organizations = List.of(
        			Organization.builder().name("Pikashu").blocked(false).blockedBy(null).build(),
        			SQUANCHY,
        			GARFIELD,
        			FRAJOLA,
        			TOM,
        			HELLO_KITTY
        			);
        	organizationRepository.saveAll(organizations);

            List<Employee> pikashuEmployees = List.of(
            		Employee.builder().firstName("John").lastName("Doe").organization(organizations.get(0)).build(),
            		Employee.builder().firstName("Jane").lastName("Smith").organization(organizations.get(0)).build(),
            		Employee.builder().firstName("Creed").lastName("Braton").organization(organizations.get(0)).build()
            		);
            employeeRepository.saveAll(pikashuEmployees);

            List<Employee> squanchyEmployees = List.of(
            	    new Employee("Michael", "Scott", organizations.get(1)),
            	    new Employee("Dwight", "Schrute", organizations.get(1)),
            	    new Employee("Jim", "Halpert", organizations.get(1)),
            	    new Employee("Pam", "Beesley", organizations.get(1))
            	);
        	employeeRepository.saveAll(squanchyEmployees);
        	
        	List<Employee> tomEmployees = List.of(
        	        new Employee("Andy", "Bernard", organizations.get(2)),
        	        new Employee("Stanley", "Hudson", organizations.get(2)),
        	        new Employee("Phyllis", "Vance", organizations.get(2)),
        	        new Employee("Meredith", "Palmer", organizations.get(2))
        	);
        	employeeRepository.saveAll(tomEmployees);
        }

        int totalAwards = employeeRepository.findAll().stream()
                .mapToInt(Employee::getDundieAwards)
                .sum();
        
        this.awardsCache.setTotalAwards(totalAwards);

        
        loadEmployesRecordsToOrganization(1,GARFIELD);
        loadEmployesRecordsToOrganization(2,HELLO_KITTY);
        
        //big data ones
        if(appProperties.enableTestBehavior()) {
        	loadEmployesRecordsToOrganization(3_000_000,FRAJOLA);
        	loadEmployesRecordsToOrganization(3_000_000,TOM);
        }else {
        	loadEmployesRecordsToOrganization(3,FRAJOLA);
            loadEmployesRecordsToOrganization(2,TOM);
        }
        
    }
    

    private void loadEmployesRecordsToOrganization(Integer quantity, Organization organization) {
    	
    	//The Office characters names ;)
    	final String[] FIRST_NAMES = {"Michael", "Jim", "Pam", "Dwight", "Ryan", "Angela", "Andy", "Kelly", "Oscar", "Stanley"};
    	final String[] LAST_NAMES = {"Scott", "Halpert", "Beesly", "Schrute", "Howard", "Martin", "Bernard", "Kapoor", "Martinez", "Hudson"};
    	
    	long start = System.currentTimeMillis();
        log.info("Started loading employees to {} Organization...",organization.getName());

        Random random = new Random();
        final int[] totalAwards = {0};
        List<Employee> employees = LongStream.rangeClosed(1, quantity)
                .mapToObj(id ->{ 
                	int dundieAwards = ThreadLocalRandom.current().nextInt(0, 13);
                    totalAwards[0] += dundieAwards;
                	return Employee.builder()
                        .firstName(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)])
                        .lastName(LAST_NAMES[random.nextInt(LAST_NAMES.length)])
                        .dundieAwards(dundieAwards)
                        .organization(organization)
                        .build();})
                .toList();
        
        employeeRepository.saveAll(employees);
        employeeRepository.flush();

        this.awardsCache.setTotalAwards(awardsCache.getTotalAwards()+totalAwards[0]);
        
        long end = System.currentTimeMillis();
        log.info("Completed loading employees to {} Organization in {} seconds", organization.getName(), String.format("%.2f", (end - start) / 1000.0));
    
    }
    
}
