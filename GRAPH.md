# System Architecture Graph

This document provides a visual representation of the LinkedIn clone microservices architecture.

```mermaid
graph TD
    subgraph Client
        FE[React Frontend]
    end

    subgraph Infrastructure
        GW[API Gateway :9191]
        DS[Discovery Service :8761]
        KC[Keycloak :8080]
        KF[Kafka :9092]
        KUI[Kafka UI :8088]
        RD[Redis :6379]
        ES[Elasticsearch :9200]
        KB[Kibana :5601]
        MI[MinIO :9000/9001]
        ZK[Zipkin :9411]
        PR[Prometheus :9090]
        GR[Grafana :3000]
    end

    subgraph Microservices
        US[User Service :8090]
        PS[Profile Service :8700]
        CS[Chat Service :8085]
        JS[Job Service :8081]
        NS[Notification Service :9095]
        SS[Search Service :8087]
    end

    subgraph Databases
        DB_PG[(PostgreSQL :5432)]
    end

    %% Client to Gateway
    FE --> GW

    %% Gateway Routing & Auth
    GW --> DS
    GW --> KC
    GW --> US
    GW --> PS
    GW --> CS
    GW --> JS
    GW --> NS
    GW --> SS

    %% Kafka ecosystem
    KF --- KUI

    %% Inter-service Sync (Feign)
    US <--> PS
    PS <--> JS
    
    %% Inter-service Async (Kafka)
    US -- Event --> KF
    PS -- Event --> KF
    JS -- Event --> KF
    KF -- Consume --> NS
    KF -- Consume --> SS

    %% Service to Storage/DB
    US --> DB_PG
    PS --> DB_PG
    CS --> DB_PG
    JS --> DB_PG
    NS --> DB_PG

    US --> RD
    PS --> RD
    JS --> RD
    
    SS --> ES
    ES --- KB
    
    PS --> MI
    US --> MI
    
    %% Monitoring & Tracing
    US & PS & CS & JS & NS & SS & GW --> ZK
    US & PS & CS & JS & NS & SS & GW --> PR
    PR --> GR

    %% Shared Libraries
    subgraph Shared_Libs
        CM[Common Models]
        CU[Common Utility]
    end
    
    US -.-> CM
    PS -.-> CM
    JS -.-> CM
    NS -.-> CM
    SS -.-> CM
    
    US -.-> CU
    PS -.-> CU
    JS -.-> CU
```

## Component Overview

| Component | Description | Port |
| :--- | :--- | :--- |
| **Frontend** | React application providing the user interface. | 3000 |
| **API Gateway** | Entry point for all client requests, handles routing and security. | 9191 |
| **Discovery Service** | Eureka-based service discovery for microservices. | 8761 |
| **Keycloak** | Centralized identity and access management. | 8080 |
| **Kafka** | Message broker for asynchronous, event-driven communication. | 9092 |
| **Kafka UI** | Web interface for managing Kafka topics and messages. | 8088 |
| **Redis** | In-memory data store used for caching. | 6379 |
| **Elasticsearch** | Distributed search and analytics engine. | 9200 |
| **Kibana** | Visualization dashboard for Elasticsearch data. | 5601 |
| **MinIO** | Object storage for file uploads (Console on 9001). | 9000 |
| **Zipkin** | Distributed tracing system to monitor microservice latency. | 9411 |
| **Prometheus** | Monitoring system that collects metrics from services. | 9090 |
| **Grafana** | Analytics and monitoring dashboard for Prometheus metrics. | 3000 |
| **Microservices** | Domain-specific services (User, Profile, Chat, Job, Notification, Search). | Varies |
| **Common Models** | Shared JPA entities and DTOs to ensure data consistency. | - |
| **Common Utility** | Shared logic for Feign clients, Kafka publishing, and S3 storage. | - |
| **PostgreSQL** | Primary relational database for persistence. | 5432 |
