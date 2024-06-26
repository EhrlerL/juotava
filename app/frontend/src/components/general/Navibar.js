import { useState } from 'react';
import { NavLink } from 'react-router-dom';

import { Col, NavDropdown, Nav, Navbar } from 'react-bootstrap';

import LogInIndicator from '../auth/LogInIndicator';

import { setTheme, setStoredTheme, getStoredTheme } from '../../App';

import logo from "../../icons/Juotava_Draft_Icon.png";

function Navibar(props) {
  
    const getIcon = (storedTheme) => {
      switch (storedTheme) {
        case 'light':
          return <span className="material-icons icon-align">light_mode</span>;
        case 'dark':
          return <span className="material-icons icon-align">dark_mode</span>;
        case 'auto':
          return <span className="material-icons icon-align">contrast</span>;
        default:
          return <span className="material-icons icon-align">contrast</span>;
      }
    }
    const [mode, setMode] = useState(getIcon(getStoredTheme()));
    const handleSetTheme = (theme) => {
      setTheme(theme);
      setStoredTheme(theme);
      setMode(getIcon(theme));
    }

    return (
      <Navbar sticky="top" key={false} bg="primary" variant="dark" className="mb-3">
        <Col className="ms-3">
        <Nav>
          <Navbar.Brand as={NavLink} to="/">
            <img src={logo} alt="Logo" style={{height: '30px', width: '30px'}} className="d-inline-block align-top"/>
          </Navbar.Brand>
        </Nav>
        </Col>
        <Col className="d-flex justify-content-center">
        <Nav>
          <Nav.Link as={NavLink} to="/browser" className="d-flex align-items-center">
            <span className="material-icons">
              search
            </span>
          </Nav.Link>
          <Nav.Link as={NavLink} to="/composer" className="d-flex align-items-center">
            <span className="material-icons">
              add_circle_outline
            </span>
          </Nav.Link>
          <Nav.Link as={NavLink} to="/bartinder" className="d-flex align-items-center">
            <span className="material-icons">
              local_fire_department
            </span>
          </Nav.Link>
        </Nav>
        </Col>
        <Col className="d-flex justify-content-end me-2">
        <Nav>
          <NavDropdown align="end" title={mode}>
              <NavDropdown.Item active={getStoredTheme() === "light"} onClick={() => handleSetTheme("light")} className="d-flex align-items-center"><span className="material-icons me-2">light_mode</span> Light Mode</NavDropdown.Item>
              <NavDropdown.Item active={getStoredTheme() === "dark"} onClick={() => handleSetTheme("dark")} className="d-flex align-items-center"><span className="material-icons me-2">dark_mode</span> Dark Mode</NavDropdown.Item>
              <NavDropdown.Item active={getStoredTheme() === "auto"} onClick={() => handleSetTheme("auto")} className="d-flex align-items-center"><span className="material-icons me-2">contrast</span> Auto Mode</NavDropdown.Item>
          </NavDropdown>
          <LogInIndicator />
        </Nav>
        </Col>
      </Navbar>
    );
}
export default Navibar;