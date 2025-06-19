# Portfolio: Kevyt projektinhallintamalli

Tämä on esimerkki kevytprojektinhallintamallista, jossa tiimit ja projektit toimivat domain-aggregaatteina. Projektin tavoitteena on havainnollistaa domain-keskeistä arkkitehtuuria, jossa liiketoimintasäännöt asuvat aggregaateissa, ei serviceissä.


## Tavoite

- Rakentaa testattava ja ymmärrettävä DDD-pohjainen malli.
- Korostaa domainin eheyttä (esim. TimeEstimation ei ole vain `int`, vaan ValueObject).
- Näyttää miten write- ja read-mallit voidaan erottaa kevyesti.
- Näyttää miten Spring Data JDBC tukee immutable-struktuuria
- Käyttää tapahtumia (esim. `TeamTaskCompletedEvent`) tilan päivittämiseen aggregaattien välillä.


## Teknologiat

- Java 17
- Maven
- PostgreSQL
- Docker
- Spring Boot
- Spring Data JDBC
- JUnit 5 + Mockito

## Peruskäyttö

Sovellus vaatii PostgreSQL:n, joka ajetaan Dockerin kautta. docker-compose.yml on konfiguroitu seuraavasti:

### .env-tiedosto (luo juureen)
POSTGRES_PASSWORD=salasana123

PGDATA_VOLUME=/c/Users/demo/postgres-data

#### PostgreSql-kontin käynnistys
```docker compose up -d```

### Spring-boot 
Sovellus olettaa ympäristömuuttujista löytyvän tietokannan salasanan. Sen voi Windowsin cmd-promptissa asettaa esimerkiksi näin ```set POSTGRES_PASSWORD=salasana123```
Itse sovellus käynnistetään project-demo-kansiossa ajamalla komento ```mvn spring-boot:run```

## Domainin rakenne

- **Project**: Omistaa alkuarvion (InitialEstimation), projektille lisätään tehtäviä. Projekti itse huolehtii tehtäviä lisättäessä, että arvioitu aika-arvio ei ylity
- **Team**: Omistaa tiimin jäsenet ja vastaa tehtävien hallinnasta.
- **ProjectTask**: On osa projektisuunnittelua. Se määrittää mitä pitää tehdä ja kuinka paljon työtä siihen on alun perin arvioitu.
- **TeamTask**: Edustaa sitä, miten tiimi toteuttaa projektitehtävän: kuka tekee sen, missä vaiheessa se on, ja paljonko todellista aikaa kului.

## Value Objectit

- **TimeEstimation**: Abstraktoi ajan arvion. Estää virheelliset arvot (esim. negatiiviset tunnit).
- **ActualSpentTime**: Kuvaa oikeasti kulunutta aikaa. Voi päivittyä vasta kun task on valmis.
- **ProjectId, ProjectTaskId, ContactPersonId, TaskId, TeamId, TeamTaskId, TeamMemberId**: Varmistavat oikeat ID-käytännöt ilman paljaita merkkijonoja tai UUID:itä.


## Eventit

Tietyt aggregaattitapahtumat laukaisevat muita päivityksiä järjestelmässä:

- `TaskAddedToProjectEvent`: syntyy, kun uusi taski lisätään projektille, käsittelijä lähettää tästä sähköpostia projektin yhteyshenkilölle. Tämä demonstroi "side-effect":in käsittelyä
- `TeamTaskCompletedEvent`: kun tiimi merkitsee tehtävän valmiiksi, tämän eventin käsittelijä päivittää projektin vastaavan taskin valmiiksi toteutuneen työmäärän kanssa. Projekti itse huolehtii itse siitä, että projekti merkitään valmiiksi jos kaikki sen tehtävät ovat valmiita. Tämän eventin käsittely demonstroi DDD:n perusperiaatetta, että kahta aggregate roottia ei saa tallentaa yhdessä transaktiossa. Eventin käsittely on myös idempotentti. Jos sen käsittelyn aikana tapahtuu optimistisen lukituksen virhe, yritetään uudestaan. Jos puolestaan toinen osapuoli on yrittänyt lisätä tehtävää, tarkistetaan onko projekti jo valmis ja hylätään sen aiheuttama päivitys (jos projekti on jo valmis)

## REST-endpointit (esimerkit)

### Luo projekti
```curl --location 'http://localhost:8080/projects' --header 'Content-Type: application/json' --data-raw '{"name":"coding project", "description":"portfolio demonstration", "estimatedEndDate": "2026-01-01", "estimation":{"hours":10,"minutes":55}, "contactPersonInput":{"name":"tapio niemelä","email":"tapio.niemela_1@yahoo.com"}}'```

### Lisää taski projektille
```curl --location 'http://localhost:8080/projects/cd8a4243-717b-4181-bb5a-83381f511920/tasks' --header 'Content-Type: application/json' --data '{"name":"java code", "description":"make java code demonstrating ddd and spring data jdbc", "estimation":{"hours":8, "minutes":0}}'```

### Lisää tiimi
```curl --location 'http://localhost:8080/teams' --header 'Content-Type: application/json' --data '{"name":"ddd and spring data jdbc demonstration team"}'```

### Lisää tiimille jäsen
```curl --location 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b/members' --header 'Content-Type: application/json' --data '{"name":"tapio niemelä", "profession":"ddd enthuistic"}'```

### Lisää (projektin) taski tiimille
```curl --location --request POST 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b/tasks/by-project-id/6e46e573-1bf4-46e9-a633-fb7447e42c16' --data ''```

### Assignoi taski tiimin jäsenelle
```curl --location --request PATCH 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b/tasks/5ad12dec-34be-40a9-ab9a-0c619b6ae6ab/assignee' --header 'Content-Type: application/json' --data '{"assigneeId":"c41c9a87-688f-428d-a1d5-4134f1faeeaf"}'```

### Ota taski käsittelyyn
```curl --location --request POST 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b/tasks/5ad12dec-34be-40a9-ab9a-0c619b6ae6ab/mark-in-progress' --data ''```

### Merkitse taski valmiiksi
```curl --location 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b/tasks/f078044f-72dc-4f70-9230-affece4758db/complete' --header 'Content-Type: application/json' --data '{"hours":2, "minutes":0}'```

### Unassignoi task
```curl --location 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b/tasks/a41eb504-ae94-40d7-a38f-e0cd2217e5f9/unassign' --header 'Content-Type: application/json' --data ''```

### Poista annettu task tiimiltä
```curl --location --request DELETE 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b/tasks/a41eb504-ae94-40d7-a38f-e0cd2217e5f9'```

### Hae annettu projekti
```curl --location 'http://localhost:8080/projects/cd8a4243-717b-4181-bb5a-83381f511920' --data ''```

### Hae annettu tiimi
```curl --location 'http://localhost:8080/teams/791031a6-922b-4ea0-93da-ae7b21a7a09b' --data ''```

## Rajoitteet ja huomiot

- Tämä projekti demonstroi lähinnä DDD ja Spring Data JDBC-osaamista. Siinä ei ole toteutettu mm. oikeaa autentikoitumista tai minkäänlaista käyttöliittymää.
- Tavoitteena on ollut pitää aggregate-malli keskittyneenä toimintoihin (write). Tietojen hakeminen(read) on toteutettu erikseen suorilla SQL-kyselyillä. Read-malli on tehty kevyesti, koska se ei ole oleellinen osa demoa
- Yksittäisen projektin hakeminen palauttaa näkymän jossa sen sisältämien taskien aikamääreitä on laskettu yhteen. Toinen tapa toteuttaa vastaava olisi ollut kirjoittaa ne tietokantaan päivitysten yhteydessä; tässä valittiin kuitenkin yksinkertaisempi tapa
- Yksikkötestit on tehty vain kriittisille toiminnallisuuksille

## Kehittäjä

- Toteuttanut Tapio Niemelä. Portfolio toimii todisteena osaamisesta:
- Java + Spring Boot + Spring Data JDBC
- Domain Driven Design (aggregaatit, säännöt, eventit)
- Testivetoisuus
- Käytännöllinen REST-rajapinta
