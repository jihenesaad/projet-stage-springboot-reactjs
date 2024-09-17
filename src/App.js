import React from 'react';
import './App.css';
import SidebarComponent from './backend/SidebarComponent';
import AlltempbackComponent from './backend/AlltempbackComponent';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import AllFrontComponent from './frontend/allfrontComponent';
import RegisterComponent from './frontend/registerComponent';
import SignInComponent from './frontend/signinComponent';
import ClientComponent from './frontend/ClientComponent';
import InsightVmComponent from './backend/InsightVmComponent'
import Sites from './backend/SitesComponent';
import ArchiveComponent from './backend/Archive';

function App() {
  return (
    <div className="App">
      <Router>
        <div className="App">
          <main>
            <Routes>
              <Route path="/" element={<AllFrontComponent />} />
              <Route path="/side" element={<SidebarComponent />} />
              <Route path="/front" element={<AllFrontComponent />} />
              <Route path="/back" element={<InsightVmComponent />} />
              <Route path="/register" element={<RegisterComponent />} />
              <Route path="/signin" element={<SignInComponent />} />
              <Route path="/client" element={<ClientComponent />} />
             <Route path="/ins/:siteName" element={<InsightVmComponent/>}/>
             <Route path="/ar" element={<ArchiveComponent/>} />
             <Route path="/sites" element={<Sites/>}/>
            </Routes>
          </main>
        </div>
      </Router>
    </div>
  );
}

export default App;
