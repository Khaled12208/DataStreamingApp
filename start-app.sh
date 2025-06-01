#!/bin/bash

# Colors for better visual experience
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Function to display ASCII art
show_ascii_art() {
    echo -e "${CYAN}"
    echo "██╗  ██╗██╗  ██╗ █████╗ ██╗     ███████╗██████╗ "
    echo "██║ ██╔╝██║  ██║██╔══██╗██║     ██╔════╝██╔══██╗"
    echo "█████╔╝ ███████║███████║██║     █████╗  ██║  ██║"
    echo "██╔═██╗ ██╔══██║██╔══██║██║     ██╔══╝  ██║  ██║"
    echo "██║  ██╗██║  ██║██║  ██║███████╗███████╗██████╔╝"
    echo "╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝╚═════╝ "
    echo -e "${NC}"
    echo -e "${WHITE}          Data Streaming Application${NC}"
    echo -e "${PURPLE}    Real-time Order Processing & Analytics${NC}"
    echo ""
}

# Function to show loading animation
loading_animation() {
    local duration=$1
    local message=$2
    local chars="⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"
    local i=0
    
    while [ $i -lt $duration ]; do
        printf "\r${YELLOW}${chars:$((i % ${#chars})):1} ${message}${NC}"
        sleep 0.1
        ((i++))
    done
    printf "\r${GREEN}✓ ${message}${NC}\n"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
        exit 1
    fi
}

# Function to check service health
check_service_health() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${YELLOW}🔍 Checking ${service_name} health...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ ${service_name} is healthy!${NC}"
            return 0
        fi
        printf "\r${YELLOW}⏳ Waiting for ${service_name}... (${attempt}/${max_attempts})${NC}"
        sleep 2
        ((attempt++))
    done
    
    echo -e "\n${RED}❌ ${service_name} failed to start within expected time${NC}"
    return 1
}

# Main execution
main() {
    clear
    show_ascii_art
    
    echo -e "${WHITE}🚀 Starting Data Streaming Application...${NC}\n"
    
    # Check Docker
    echo -e "${BLUE}📋 Pre-flight checks...${NC}"
    check_docker
    echo -e "${GREEN}✓ Docker is running${NC}"
    
    # Clean previous containers
    echo -e "\n${BLUE}🧹 Cleaning up previous containers...${NC}"
    docker-compose down > /dev/null 2>&1
    loading_animation 20 "Stopping existing containers"
    
    # Build and start services
    echo -e "\n${BLUE}🔨 Building and starting services...${NC}"
    loading_animation 30 "Building application images"
    
    echo -e "${YELLOW}🔄 Starting Docker Compose services...${NC}"
    docker-compose up -d --build
    
    echo -e "\n${BLUE}🏥 Health checks...${NC}"
    
    # Check each service
    loading_animation 50 "Initializing Kafka infrastructure"
    check_service_health "Kafka UI" "http://localhost:8080"
    
    loading_animation 30 "Starting Flink cluster"
    check_service_health "Flink Dashboard" "http://localhost:8082"
    
    loading_animation 40 "Starting Order Producer"
    check_service_health "Order Producer" "http://localhost:8081/actuator/health"
    
    loading_animation 50 "Starting Analytics Consumer"
    check_service_health "Analytics Consumer" "http://localhost:8083/actuator/health"
    
    echo -e "\n${GREEN}🎉 Application successfully started!${NC}\n"
    
    # Display service URLs
    echo -e "${WHITE}📊 Available Services:${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}🌐 Order Producer API:${NC}      http://localhost:8081"
    echo -e "${YELLOW}📈 Analytics API:${NC}          http://localhost:8083"
    echo -e "${YELLOW}🔍 Kafka UI:${NC}               http://localhost:8080"
    echo -e "${YELLOW}⚡ Flink Dashboard:${NC}        http://localhost:8082"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    echo -e "\n${WHITE}📖 Quick Start:${NC}"
    echo -e "${GREEN}• Create an order:${NC} curl -X POST http://localhost:8081/orders \\"
    echo -e "                    -H 'Content-Type: application/json' \\"
    echo -e "                    -d '{\"orderId\":\"test-1\", \"customerId\":\"cust-1\", \"amount\":150.00, \"timestamp\":$(date +%s)000, \"items\":[\"laptop\"]}'"
    echo -e "${GREEN}• View analytics:${NC}  curl http://localhost:8083/analytics/sales"
    echo -e "${GREEN}• Import Postman:${NC}  docs/postman_collection.json"
    
    echo -e "\n${PURPLE}💡 Use 'docker-compose logs [service-name]' to view logs${NC}"
    echo -e "${PURPLE}💡 Use 'docker-compose down' to stop all services${NC}\n"
}

# Execute main function
main "$@" 