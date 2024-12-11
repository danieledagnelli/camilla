# CAMILLA - Contextual Autopsy Mapping Integrated Linking Layer
<img width="1519" alt="image" src="https://github.com/danieledagnelli/camilla/assets/991178/b98a1ac1-baf6-4426-8ac8-34ca198dc872">

(Below a LLM-generated summary based on the Thesis PDF)

### Author
**Daniele D'Agnelli**  
Abertay University, School of Design and Informatics, Dundee, UK  

---

## Overview
**CAMILLA** is a plugin developed for the **Autopsy** platform, aimed at simplifying and standardizing the creation of visualizations for digital forensic (DF) investigations. The goal is to enhance reporting quality, reduce subjective biases, and promote fairness in the judicial process.

---

## Objectives
1. Develop a plugin for Autopsy to create entity-relationship visualizations.
2. Support interactive and standard visualization of forensic artefacts.
3. Enable seamless export and integration of these visualizations into reports.

---

## Features
### Core Capabilities
- Drag-and-drop interaction for artefacts onto a canvas.
- Create and edit relationships between artefacts.
- Export visualizations to image formats.
- Persistent storage of visualizations in SQLite for seamless case reopening.

### Key Functionalities
- Entity graph visualization using **JGraphX**.
- Annotation support for visual relationships.
- Integration with Autopsy’s UI components and taxonomy.

---

## Architecture
### Autopsy Components
- **Operating System Compatibility**: Developed and tested on Windows.
- **Plugin Type**: Implements a **Result Viewer** for artefact relationships.
- **Backend**: Uses **SQLite** for persistent storage.
- **Frontend**: Built on **NetBeans** and Java’s Swing framework.

### CAMILLA Structure
- Modular Java packages for scalability and maintainability.
- Decoupled dependency on JGraphX for future-proofing.

---

## Results
The CAMILLA plugin has successfully:
- Eliminated the need for external tools (e.g., PowerPoint).
- Maintained a 1:1 traceability with artefacts in the case.
- Enabled richer, more accurate forensic reports with minimal effort.

---

## Future Enhancements
1. **User Experience**:
   - Fully synchronize canvas interactions with Autopsy’s Content Viewer.
   - Enable bulk artefact drag-and-drop.
2. **Graph Enhancements**:
   - Retain artefact references during graph serialization.
3. **New Visualization Types**:
   - Develop timeline visualization capabilities.
4. **Technology Updates**:
   - Migrate to actively maintained graph libraries.
   - Explore web technologies for UI modernization.
5. **AI Integration**:
   - Use Large Language Models (LLMs) for automated report generation based on visualizations.

---

## Proof of Concept: LLM Integration
By leveraging tools like ChatGPT-4, CAMILLA demonstrated the feasibility of transforming graph data into comprehensive forensic reports. This approach could significantly reduce the workload for DF professionals, allowing them to focus on analysis over documentation.

---

## Conclusion
CAMILLA bridges a critical gap in digital forensics by enabling standardized, interactive, and accessible visualizations. It sets the groundwork for future innovations in forensic reporting and case analysis.


