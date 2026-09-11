import { Routes, Route, useNavigate } from "react-router-dom";

import Sender from "./pages/Sender";
import Receiver from "./pages/Receiver";

import "./App.css";


function Home() {

  const navigate = useNavigate();

  return (
    <div className="home-page">

      <div className="home-container">

        {/* Logo */}

        <div className="logo">
          Remote<span>Support</span>
        </div>


        {/* Main Card */}

        <div className="main-card">

          <div className="main-icon">
            🖥️
          </div>


          <h1>
            Welcome to Remote Support
          </h1>


          <p className="subtitle">
            Choose your role to continue
          </p>


          {/* Role Options */}

          <div className="role-options">


            {/* SENDER */}

            <button
              className="role-option"
              onClick={() => navigate("/sender")}
            >

              <div className="role-icon sender-icon">
                🖥️
              </div>


              <div className="role-info">

                <h2>
                  Sender
                </h2>

                <p>
                  Share your screen with
                  another computer
                </p>

              </div>


              <div className="role-arrow">
                →
              </div>

            </button>


            {/* RECEIVER */}

            <button
              className="role-option"
              onClick={() => navigate("/receiver")}
            >

              <div className="role-icon receiver-icon">
                👤
              </div>


              <div className="role-info">

                <h2>
                  Receiver
                </h2>

                <p>
                  Connect to another
                  computer
                </p>

              </div>


              <div className="role-arrow">
                →
              </div>

            </button>


          </div>

        </div>


        {/* Security */}

        <div className="security">
          🔒 Secure connection &nbsp; • &nbsp;
          Permission required
        </div>


      </div>

    </div>
  );
}


/* ================================
   ROUTES
================================ */

function App() {

  return (

    <Routes>

      {/* Main Page */}

      <Route
        path="/"
        element={<Home />}
      />


      {/* Sender Page */}

      <Route
        path="/sender"
        element={<Sender />}
      />


      {/* Receiver Page */}

      <Route
        path="/receiver"
        element={<Receiver />}
      />

    </Routes>

  );
}


export default App;